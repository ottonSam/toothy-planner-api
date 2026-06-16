package br.com.ottonsam.toothy_planner_api.diet.usecases;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
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
public class DeepSeekFoodNutritionAiClient implements FoodNutritionAiClient {

    private static final MediaType JSON = MediaType.get("application/json");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public DeepSeekFoodNutritionAiClient(
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
    public FoodNutritionData lookup(String foodName) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "DeepSeek API key is not configured");
        }
        try {
            var body = objectMapper.writeValueAsString(Map.of(
                    "model",
                    model,
                    "messages",
                    List.of(Map.of("role", "user", "content", prompt(foodName))),
                    "temperature",
                    0.1));
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
                return parseContent(extractContent(objectMapper.readTree(responseBody)));
            }
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "DeepSeek nutrition lookup failed");
        }
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

    private String errorMessage(int statusCode, String responseBody) {
        if (statusCode == 401 || statusCode == 403) {
            return "DeepSeek authentication failed";
        }
        var deepSeekMessage = deepSeekMessage(responseBody);
        if (deepSeekMessage == null || deepSeekMessage.isBlank()) {
            return "DeepSeek nutrition lookup failed";
        }
        return "DeepSeek nutrition lookup failed: " + deepSeekMessage;
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
