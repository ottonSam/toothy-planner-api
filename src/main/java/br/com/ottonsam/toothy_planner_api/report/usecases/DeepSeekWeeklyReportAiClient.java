package br.com.ottonsam.toothy_planner_api.report.usecases;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekWeeklyReportAiClient implements WeeklyReportAiClient {

    private static final MediaType JSON = MediaType.get("application/json");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public DeepSeekWeeklyReportAiClient(
            ObjectMapper objectMapper,
            @Value("${deepseek.api-key:}") String apiKey,
            @Value("${deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${deepseek.model:deepseek-chat}") String model) {
        this.httpClient = new OkHttpClient();
        this.objectMapper = objectMapper.copy();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    @Override
    public String generate(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "DeepSeek API key is not configured");
        }
        try {
            var body = objectMapper.writeValueAsString(Map.of(
                    "model",
                    model,
                    "messages",
                    List.of(Map.of("role", "user", "content", prompt)),
                    "temperature",
                    0.3));
            var request = new Request.Builder()
                    .url(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(body, JSON))
                    .build();
            try (var response = httpClient.newCall(request).execute()) {
                var responseBody = response.body().string();
                if (!response.isSuccessful()) {
                    throw new ApiException(HttpStatus.BAD_GATEWAY, errorMessage(response.code(), responseBody));
                }
                return extractContent(objectMapper.readTree(responseBody));
            }
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "DeepSeek report generation failed");
        }
    }

    private String errorMessage(int statusCode, String responseBody) {
        if (statusCode == 401 || statusCode == 403) {
            return "DeepSeek authentication failed";
        }
        var deepSeekMessage = deepSeekMessage(responseBody);
        if (deepSeekMessage == null || deepSeekMessage.isBlank()) {
            return "DeepSeek report generation failed";
        }
        return "DeepSeek report generation failed: " + deepSeekMessage;
    }

    private String deepSeekMessage(String responseBody) {
        try {
            return objectMapper
                    .readTree(responseBody)
                    .path("error")
                    .path("message")
                    .asText();
        } catch (IOException exception) {
            return "";
        }
    }

    private String extractContent(JsonNode response) {
        var content =
                response.path("choices").path(0).path("message").path("content").asText();
        if (content == null || content.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "DeepSeek report generation failed");
        }
        return content;
    }
}
