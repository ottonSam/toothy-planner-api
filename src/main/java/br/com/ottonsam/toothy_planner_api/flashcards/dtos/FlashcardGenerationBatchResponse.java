package br.com.ottonsam.toothy_planner_api.flashcards.dtos;

import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardGenerationBatchEntity;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardGenerationStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FlashcardGenerationBatchResponse(
        UUID id,
        int batchNumber,
        int requestedCount,
        int createdCount,
        FlashcardGenerationStatus status,
        String errorMessage,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        OffsetDateTime updatedAt) {

    public static FlashcardGenerationBatchResponse from(FlashcardGenerationBatchEntity batch) {
        return new FlashcardGenerationBatchResponse(
                batch.getId(),
                batch.getBatchNumber(),
                batch.getRequestedCount(),
                batch.getCreatedCount(),
                batch.getStatus(),
                batch.getErrorMessage(),
                batch.getCreatedAt(),
                batch.getStartedAt(),
                batch.getFinishedAt(),
                batch.getUpdatedAt());
    }
}
