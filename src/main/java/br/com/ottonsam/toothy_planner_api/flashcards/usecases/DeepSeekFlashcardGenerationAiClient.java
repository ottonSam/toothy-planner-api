package br.com.ottonsam.toothy_planner_api.flashcards.usecases;

import br.com.ottonsam.toothy_planner_api.ai_usage.entities.AiFeature;
import br.com.ottonsam.toothy_planner_api.ai_usage.usecases.DeepSeekChatCompletionRequest;
import br.com.ottonsam.toothy_planner_api.ai_usage.usecases.DeepSeekChatCompletionUseCase;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardDeckType;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardExampleData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekFlashcardGenerationAiClient implements FlashcardGenerationAiClient {

    private static final int MAX_OUTPUT_TOKENS = 8192;

    private final ObjectMapper objectMapper;
    private final DeepSeekChatCompletionUseCase chatCompletionUseCase;

    public DeepSeekFlashcardGenerationAiClient(
            ObjectMapper objectMapper, DeepSeekChatCompletionUseCase chatCompletionUseCase) {
        this.objectMapper = objectMapper.copy();
        this.chatCompletionUseCase = chatCompletionUseCase;
    }

    @Override
    public FlashcardGenerationAiResult generate(UUID userId, FlashcardGenerationAiRequest request) {
        try {
            var response = chatCompletionUseCase.execute(new DeepSeekChatCompletionRequest(
                    userId,
                    AiFeature.FLASHCARD_GENERATION,
                    List.of(
                            Map.of("role", "system", "content", systemPrompt()),
                            Map.of("role", "user", "content", userPrompt(request))),
                    0.2,
                    MAX_OUTPUT_TOKENS,
                    "DeepSeek flashcard generation failed",
                    "DeepSeek flashcard generation timed out"));
            return parseContent(request.type(), extractContent(response));
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
