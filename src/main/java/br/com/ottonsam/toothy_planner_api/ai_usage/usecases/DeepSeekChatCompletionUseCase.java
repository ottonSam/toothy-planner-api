package br.com.ottonsam.toothy_planner_api.ai_usage.usecases;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DeepSeekChatCompletionUseCase {

    private static final MediaType JSON = MediaType.get("application/json");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AiTokenUsageUseCase tokenUsageUseCase;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public DeepSeekChatCompletionUseCase(
            ObjectMapper objectMapper,
            AiTokenUsageUseCase tokenUsageUseCase,
            @Value("${deepseek.api-key:}") String apiKey,
            @Value("${deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${deepseek.model:deepseek-chat}") String model,
            @Value("${deepseek.timeout-seconds:60}") long timeoutSeconds) {
        var safeTimeoutSeconds = Math.max(1, timeoutSeconds);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .writeTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(safeTimeoutSeconds))
                .callTimeout(Duration.ofSeconds(safeTimeoutSeconds + 15))
                .build();
        this.objectMapper = objectMapper.copy();
        this.tokenUsageUseCase = tokenUsageUseCase;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    public JsonNode execute(DeepSeekChatCompletionRequest completionRequest) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "DeepSeek API key is not configured");
        }
        var body = serializeRequest(completionRequest);
        var reservationId = tokenUsageUseCase.reserve(
                completionRequest.userId(), completionRequest.feature(), body, completionRequest.maxTokens());
        var request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body, JSON))
                .build();
        try (var response = httpClient.newCall(request).execute()) {
            var responseBody = response.body().string();
            if (!response.isSuccessful()) {
                tokenUsageUseCase.release(reservationId);
                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        errorMessage(completionRequest.failureMessage(), response.code(), responseBody));
            }
            var json = objectMapper.readTree(responseBody);
            tokenUsageUseCase.charge(reservationId, json.path("usage"));
            return json;
        } catch (SocketTimeoutException exception) {
            tokenUsageUseCase.chargeReserved(reservationId);
            throw new ApiException(HttpStatus.BAD_GATEWAY, completionRequest.timeoutMessage());
        } catch (ApiException exception) {
            throw exception;
        } catch (IOException exception) {
            tokenUsageUseCase.chargeReserved(reservationId);
            throw new ApiException(HttpStatus.BAD_GATEWAY, completionRequest.failureMessage());
        }
    }

    private String serializeRequest(DeepSeekChatCompletionRequest completionRequest) {
        try {
            var request = new LinkedHashMap<String, Object>();
            request.put("model", model);
            request.put("messages", completionRequest.messages());
            request.put("temperature", completionRequest.temperature());
            request.put("max_tokens", completionRequest.maxTokens());
            return objectMapper.writeValueAsString(request);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, completionRequest.failureMessage());
        }
    }

    private String errorMessage(String failureMessage, int statusCode, String responseBody) {
        if (statusCode == 401 || statusCode == 403) {
            return "DeepSeek authentication failed";
        }
        try {
            var providerMessage = objectMapper
                    .readTree(responseBody)
                    .path("error")
                    .path("message")
                    .asText();
            return providerMessage == null || providerMessage.isBlank()
                    ? failureMessage
                    : failureMessage + ": " + providerMessage;
        } catch (IOException exception) {
            return failureMessage;
        }
    }
}
