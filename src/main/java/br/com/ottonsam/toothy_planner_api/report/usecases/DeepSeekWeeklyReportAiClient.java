package br.com.ottonsam.toothy_planner_api.report.usecases;

import br.com.ottonsam.toothy_planner_api.ai_usage.entities.AiFeature;
import br.com.ottonsam.toothy_planner_api.ai_usage.usecases.DeepSeekChatCompletionRequest;
import br.com.ottonsam.toothy_planner_api.ai_usage.usecases.DeepSeekChatCompletionUseCase;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekWeeklyReportAiClient implements WeeklyReportAiClient {

    private static final int MAX_OUTPUT_TOKENS = 4096;

    private final DeepSeekChatCompletionUseCase chatCompletionUseCase;

    public DeepSeekWeeklyReportAiClient(DeepSeekChatCompletionUseCase chatCompletionUseCase) {
        this.chatCompletionUseCase = chatCompletionUseCase;
    }

    @Override
    public String generate(UUID userId, String prompt) {
        var response = chatCompletionUseCase.execute(new DeepSeekChatCompletionRequest(
                userId,
                AiFeature.WEEKLY_REPORT,
                List.of(Map.of("role", "user", "content", prompt)),
                0.3,
                MAX_OUTPUT_TOKENS,
                "DeepSeek report generation failed",
                "DeepSeek report generation timed out"));
        return extractContent(response);
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
