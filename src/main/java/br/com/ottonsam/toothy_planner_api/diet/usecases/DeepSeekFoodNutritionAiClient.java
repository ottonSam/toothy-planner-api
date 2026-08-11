package br.com.ottonsam.toothy_planner_api.diet.usecases;

import br.com.ottonsam.toothy_planner_api.ai_usage.entities.AiFeature;
import br.com.ottonsam.toothy_planner_api.ai_usage.usecases.DeepSeekChatCompletionRequest;
import br.com.ottonsam.toothy_planner_api.ai_usage.usecases.DeepSeekChatCompletionUseCase;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekFoodNutritionAiClient implements FoodNutritionAiClient {

    private static final int MAX_OUTPUT_TOKENS = 1024;

    private final ObjectMapper objectMapper;
    private final DeepSeekChatCompletionUseCase chatCompletionUseCase;

    public DeepSeekFoodNutritionAiClient(
            ObjectMapper objectMapper, DeepSeekChatCompletionUseCase chatCompletionUseCase) {
        this.objectMapper = objectMapper.copy();
        this.chatCompletionUseCase = chatCompletionUseCase;
    }

    @Override
    public FoodNutritionData lookup(UUID userId, String foodName) {
        var response = chatCompletionUseCase.execute(new DeepSeekChatCompletionRequest(
                userId,
                AiFeature.FOOD_NUTRITION,
                List.of(Map.of("role", "user", "content", prompt(foodName))),
                0.1,
                MAX_OUTPUT_TOKENS,
                "DeepSeek nutrition lookup failed",
                "DeepSeek nutrition lookup timed out"));
        return parseContent(extractContent(response));
    }

    private String prompt(String foodName) {
        return """
                Voce e uma base nutricional. Retorne somente JSON valido, sem markdown.

                Para o alimento "__FOOD_NAME__", retorne calorias e macros de 1 grama e de uma porcao comum.
                A porcao nao precisa ter equivalencia em gramas.

                Formato obrigatorio:
                {
                  "name": "__FOOD_NAME__",
                  "perGram": {
                    "kcal": 0.00,
                    "protein": 0.00,
                    "carbohydrate": 0.00,
                    "fat": 0.00
                  },
                  "portion": {
                    "description": "1 porcao comum",
                    "kcal": 0.00,
                    "protein": 0.00,
                    "carbohydrate": 0.00,
                    "fat": 0.00
                  }
                }
                """.replace("__FOOD_NAME__", foodName);
    }

    private String extractContent(JsonNode response) {
        var content =
                response.path("choices").path(0).path("message").path("content").asText();
        if (content == null || content.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "DeepSeek nutrition lookup failed");
        }
        return content;
    }

    private FoodNutritionData parseContent(String content) {
        try {
            var json = objectMapper.readTree(stripMarkdownFence(content));
            var perGram = json.path("perGram");
            var portion = json.path("portion");
            return new FoodNutritionData(
                    FoodNameNormalizer.normalize(json.path("name").asText()),
                    new NutritionValues(
                            requiredDecimal(perGram, "kcal"),
                            requiredDecimal(perGram, "protein"),
                            requiredDecimal(perGram, "carbohydrate"),
                            requiredDecimal(perGram, "fat")),
                    requiredText(portion, "description"),
                    new NutritionValues(
                            requiredDecimal(portion, "kcal"),
                            requiredDecimal(portion, "protein"),
                            requiredDecimal(portion, "carbohydrate"),
                            requiredDecimal(portion, "fat")));
        } catch (IllegalArgumentException | IOException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "DeepSeek nutrition response is invalid");
        }
    }

    private String stripMarkdownFence(String content) {
        var trimmed = content.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        var firstLineEnd = trimmed.indexOf('\n');
        var lastFence = trimmed.lastIndexOf("```");
        if (firstLineEnd < 0 || lastFence <= firstLineEnd) {
            return trimmed;
        }
        return trimmed.substring(firstLineEnd + 1, lastFence).trim();
    }

    private BigDecimal requiredDecimal(JsonNode node, String field) {
        var value = node.path(field);
        if (!value.isNumber()) {
            throw new IllegalArgumentException(field);
        }
        return value.decimalValue();
    }

    private String requiredText(JsonNode node, String field) {
        var value = node.path(field).asText();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field);
        }
        return value;
    }
}
