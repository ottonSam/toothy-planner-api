package br.com.ottonsam.toothy_planner_api.financial_manager.usecases;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class FasterWhisperAudioTranscriptionClient implements ExpenseAudioTranscriptionClient {

    private static final MediaType JSON = MediaType.get("application/json");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final long timeoutSeconds;

    public FasterWhisperAudioTranscriptionClient(
            ObjectMapper objectMapper,
            @Value("${transcription.base-url:http://audio-transcriber:8000}") String baseUrl,
            @Value("${transcription.timeout-seconds:120}") long timeoutSeconds) {
        this.timeoutSeconds = Math.max(1, timeoutSeconds);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .writeTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(this.timeoutSeconds))
                .callTimeout(Duration.ofSeconds(this.timeoutSeconds + 15))
                .build();
        this.objectMapper = objectMapper.copy();
        this.baseUrl = baseUrl;
    }

    @Override
    public String transcribe(String audioBase64, String contentType) {
        try {
            var body = objectMapper.writeValueAsString(Map.of("audioBase64", audioBase64, "contentType", contentType));
            var request = new Request.Builder()
                    .url(baseUrl + "/transcribe")
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(body, JSON))
                    .build();
            try (var response = httpClient.newCall(request).execute()) {
                var responseBody = response.body().string();
                if (!response.isSuccessful()) {
                    throw new ApiException(HttpStatus.BAD_GATEWAY, transcriptionFailureMessage(responseBody));
                }
                return textFromResponse(responseBody);
            }
        } catch (SocketTimeoutException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Audio transcription timed out");
        } catch (ConnectException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Audio transcription service is unavailable");
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Audio transcription service is unavailable");
        }
    }

    private String textFromResponse(String responseBody) {
        try {
            return objectMapper.readTree(responseBody).path("text").asText();
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Audio transcription failed");
        }
    }

    private String transcriptionFailureMessage(String responseBody) {
        var detail = serviceDetail(responseBody);
        if (detail.isBlank()) {
            return "Audio transcription failed";
        }
        return "Audio transcription failed: " + detail;
    }

    private String serviceDetail(String responseBody) {
        try {
            JsonNode response = objectMapper.readTree(responseBody);
            var message = response.path("message").asText();
            if (!message.isBlank()) {
                return message;
            }
            var detail = response.path("detail").asText();
            if (!detail.isBlank()) {
                return detail;
            }
            return "";
        } catch (IOException exception) {
            return "";
        }
    }
}
