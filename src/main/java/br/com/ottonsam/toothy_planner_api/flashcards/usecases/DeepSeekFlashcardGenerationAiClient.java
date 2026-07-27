package br.com.ottonsam.toothy_planner_api.flashcards.usecases;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardDeckType;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardExampleData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
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
public class DeepSeekFlashcardGenerationAiClient implements FlashcardGenerationAiClient {

    private static final MediaType JSON = MediaType.get("application/json");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final long timeoutSeconds;

    public DeepSeekFlashcardGenerationAiClient(
            ObjectMapper objectMapper,
            @Value("${deepseek.api-key:}") String apiKey,
            @Value("${deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${deepseek.model:deepseek-chat}") String model,
            @Value("${deepseek.timeout-seconds:60}") long timeoutSeconds) {
        this.timeoutSeconds = Math.max(1, timeoutSeconds);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .writeTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(this.timeoutSeconds))
                .callTimeout(Duration.ofSeconds(this.timeoutSeconds + 15))
                .build();
        this.objectMapper = objectMapper.copy();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    @Override
    public FlashcardGenerationAiResult generate(FlashcardGenerationAiRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "DeepSeek API key is not configured");
        }
        try {
            var body = objectMapper.writeValueAsString(Map.of(
                    "model",
                    model,
                    "messages",
                    List.of(
                            Map.of("role", "system", "content", systemPrompt()),
                            Map.of("role", "user", "content", userPrompt(request))),
                    "temperature",
                    0.2));
            var httpRequest = new Request.Builder()
                    .url(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(body, JSON))
                    .build();
            try (var response = httpClient.newCall(httpRequest).execute()) {
                var responseBody = response.body().string();
                if (!response.isSuccessful()) {
                    throw new ApiException(HttpStatus.BAD_GATEWAY, errorMessage(response.code(), responseBody));
                }
                return parseContent(request.type(), extractContent(objectMapper.readTree(responseBody)));
            }
        } catch (SocketTimeoutException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "DeepSeek flashcard generation timed out");
        } catch (IOException exception) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "DeepSeek flashcard generation failed: request or response could not be read");
        }
    }

    String systemPrompt() {
        return """
                You are generating flashcards for a language-learning deck.

                Never generate a card whose target term is present in alreadyGeneratedTerms.
                The alreadyGeneratedTerms list contains every target term already created for this deck across all previous batches.
                This rule is mandatory even if the term is highly relevant to the requested context.
                All returned target terms must be unique within the response and must not appear in alreadyGeneratedTerms.

                Return only valid JSON, without markdown.
                The root JSON value must be an object containing exactly one field named cards.
                The cards field must be an array: {"cards":[...]}.
                Respect targetLanguage and baseLanguage.
                Generate exactly requestedCount unique cards when possible.
                Generate useful tags for each card.
                Generate at least one translated example for each card.
                Generate two or three examples when there is relevant ambiguity.
                Do not invent fields outside the expected schema.

                Expected VOCABULARY card fields: word, translation, phonetic, level, tags, examples, usageNote.
                Expected IRREGULAR_VERBS card fields: baseVerb, pastSimple, pastParticiple, translation, tags, examples, usageNote.
                Expected EXPRESSIONS card fields: expression, translation, tags, examples, usageNote.
                Examples must contain text and translation.
                """;
    }

    private String userPrompt(FlashcardGenerationAiRequest request) throws IOException {
        return objectMapper.writeValueAsString(Map.of(
                "type",
                request.type().name(),
                "context",
                request.context(),
                "targetLanguage",
                request.targetLanguage(),
                "baseLanguage",
                request.baseLanguage(),
                "requestedCount",
                request.requestedCount(),
                "alreadyGeneratedTerms",
                request.alreadyGeneratedTerms()));
    }

    private String errorMessage(int statusCode, String responseBody) {
        if (statusCode == 401 || statusCode == 403) {
            return "DeepSeek authentication failed";
        }
        var deepSeekMessage = deepSeekMessage(responseBody);
        if (deepSeekMessage == null || deepSeekMessage.isBlank()) {
            return "DeepSeek flashcard generation failed";
        }
        return "DeepSeek flashcard generation failed: " + deepSeekMessage;
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
            throw new ApiException(HttpStatus.BAD_GATEWAY, "DeepSeek flashcard generation failed");
        }
        return content;
    }

    FlashcardGenerationAiResult parseContent(FlashcardDeckType type, String content) {
        try {
            var json = objectMapper.readTree(stripMarkdownFence(content));
            var cards = json.isArray() ? json : json.path("cards");
            if (!cards.isArray()) {
                throw new IllegalArgumentException("cards is required");
            }
            var result = new ArrayList<GeneratedFlashcardData>();
            cards.forEach(card -> result.add(parseCard(type, card)));
            return new FlashcardGenerationAiResult(result);
        } catch (IllegalArgumentException | IOException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "DeepSeek flashcard generation response is invalid");
        }
    }

    private GeneratedFlashcardData parseCard(FlashcardDeckType type, JsonNode card) {
        return switch (type) {
            case VOCABULARY ->
                new GeneratedFlashcardData(
                        requiredText(card, "word"),
                        null,
                        null,
                        null,
                        null,
                        requiredText(card, "translation"),
                        optionalText(card, "phonetic"),
                        optionalText(card, "level"),
                        tags(card),
                        examples(card),
                        optionalText(card, "usageNote"));
            case IRREGULAR_VERBS ->
                new GeneratedFlashcardData(
                        null,
                        requiredText(card, "baseVerb"),
                        requiredText(card, "pastSimple"),
                        requiredText(card, "pastParticiple"),
                        null,
                        requiredText(card, "translation"),
                        null,
                        null,
                        tags(card),
                        examples(card),
                        optionalText(card, "usageNote"));
            case EXPRESSIONS ->
                new GeneratedFlashcardData(
                        null,
                        null,
                        null,
                        null,
                        requiredText(card, "expression"),
                        requiredText(card, "translation"),
                        null,
                        null,
                        tags(card),
                        examples(card),
                        optionalText(card, "usageNote"));
        };
    }

    private List<String> tags(JsonNode card) {
        var tags = card.path("tags");
        if (!tags.isArray()) {
            return List.of();
        }
        var result = new ArrayList<String>();
        tags.forEach(tag -> {
            var value = tag.asText();
            if (value != null && !value.isBlank()) {
                result.add(value.trim());
            }
        });
        return result;
    }

    private List<FlashcardExampleData> examples(JsonNode card) {
        var examples = card.path("examples");
        if (!examples.isArray() || examples.isEmpty()) {
            throw new IllegalArgumentException("examples is required");
        }
        var result = new ArrayList<FlashcardExampleData>();
        examples.forEach(example -> result.add(
                new FlashcardExampleData(requiredText(example, "text"), requiredText(example, "translation"))));
        return result;
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

    private String requiredText(JsonNode node, String field) {
        var value = node.path(field).asText();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String optionalText(JsonNode node, String field) {
        var value = node.path(field).asText();
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
