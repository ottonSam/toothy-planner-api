package br.com.ottonsam.toothy_planner_api.flashcards.dtos;

import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardDeckEntity;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardDeckType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FlashcardDeckResponse(
        UUID id,
        String name,
        String context,
        String targetLanguage,
        String baseLanguage,
        FlashcardDeckType type,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static FlashcardDeckResponse from(FlashcardDeckEntity deck) {
        return new FlashcardDeckResponse(
                deck.getId(),
                deck.getName(),
                deck.getContext(),
                deck.getTargetLanguage(),
                deck.getBaseLanguage(),
                deck.getType(),
                deck.getCreatedAt(),
                deck.getUpdatedAt());
    }
}
