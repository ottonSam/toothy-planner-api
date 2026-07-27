package br.com.ottonsam.toothy_planner_api.flashcards.dtos;

import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardDeckType;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardGenerationJobEntity;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardGenerationStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record FlashcardGenerationJobResponse(
        UUID id,
        UUID deckId,
        FlashcardDeckType type,
        String context,
        String targetLanguage,
        String baseLanguage,
        int requestedCount,
        int createdCount,
        FlashcardGenerationStatus status,
        String errorMessage,
        List<FlashcardGenerationBatchResponse> batches,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        OffsetDateTime updatedAt) {

    public FlashcardGenerationJobResponse {
        batches = batches == null ? List.of() : List.copyOf(batches);
    }

    public static FlashcardGenerationJobResponse from(
            FlashcardGenerationJobEntity job, List<FlashcardGenerationBatchResponse> batches) {
        return new FlashcardGenerationJobResponse(
                job.getId(),
                job.getDeck().getId(),
                job.getType(),
                job.getContext(),
                job.getTargetLanguage(),
                job.getBaseLanguage(),
                job.getRequestedCount(),
                job.getCreatedCount(),
                job.getStatus(),
                job.getErrorMessage(),
                batches,
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getFinishedAt(),
                job.getUpdatedAt());
    }

    @Override
    public List<FlashcardGenerationBatchResponse> batches() {
        return List.copyOf(batches);
    }
}
