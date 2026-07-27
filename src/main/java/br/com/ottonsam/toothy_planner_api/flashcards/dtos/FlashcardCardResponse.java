package br.com.ottonsam.toothy_planner_api.flashcards.dtos;

import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardDeckType;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record FlashcardCardResponse(
        UUID id,
        UUID deckId,
        FlashcardDeckType type,
        String word,
        String baseVerb,
        String pastSimple,
        String pastParticiple,
        String expression,
        String translation,
        String phonetic,
        String level,
        String usageNote,
        boolean active,
        OffsetDateTime lastSeenAt,
        OffsetDateTime lastReviewedAt,
        OffsetDateTime nextReviewAt,
        int reviewCount,
        int correctCount,
        int wrongCount,
        int consecutiveCorrect,
        int consecutiveWrong,
        BigDecimal difficulty,
        List<FlashcardExampleResponse> examples,
        List<FlashcardTagResponse> tags,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public FlashcardCardResponse {
        examples = examples == null ? List.of() : List.copyOf(examples);
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    @Override
    public List<FlashcardExampleResponse> examples() {
        return List.copyOf(examples);
    }

    @Override
    public List<FlashcardTagResponse> tags() {
        return List.copyOf(tags);
    }

    public static FlashcardCardResponse from(FlashcardEntity card) {
        return new FlashcardCardResponse(
                card.getId(),
                card.getDeck().getId(),
                card.getType(),
                card.getWord(),
                card.getBaseVerb(),
                card.getPastSimple(),
                card.getPastParticiple(),
                card.getExpression(),
                card.getTranslation(),
                card.getPhonetic(),
                card.getLevel(),
                card.getUsageNote(),
                card.isActive(),
                card.getLastSeenAt(),
                card.getLastReviewedAt(),
                card.getNextReviewAt(),
                card.getReviewCount(),
                card.getCorrectCount(),
                card.getWrongCount(),
                card.getConsecutiveCorrect(),
                card.getConsecutiveWrong(),
                card.getDifficulty(),
                card.getExamples().stream().map(FlashcardExampleResponse::from).toList(),
                card.getTags().stream()
                        .sorted(Comparator.comparing(tag -> tag.getName().toLowerCase(java.util.Locale.ROOT)))
                        .map(FlashcardTagResponse::from)
                        .toList(),
                card.getCreatedAt(),
                card.getUpdatedAt());
    }
}
