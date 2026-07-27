package br.com.ottonsam.toothy_planner_api.flashcards.entities;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "flashcard_generation_batches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlashcardGenerationBatchEntity {

    @Id
    @NotNull(message = "Flashcard generation batch id is required") private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    @NotNull(message = "Flashcard generation batch job is required") private FlashcardGenerationJobEntity job;

    @Column(name = "batch_number", nullable = false)
    @Min(value = 1, message = "Flashcard generation batch number must be greater than zero") private int batchNumber;

    @Column(name = "requested_count", nullable = false)
    @Min(value = 1, message = "Flashcard generation batch requested count must be greater than zero") private int requestedCount;

    @Column(name = "created_count", nullable = false)
    private int createdCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Flashcard generation batch status is required") private FlashcardGenerationStatus status;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull(message = "Updated at is required") private OffsetDateTime updatedAt;

    private FlashcardGenerationBatchEntity(FlashcardGenerationJobEntity job, int batchNumber, int requestedCount) {
        this.id = UUID.randomUUID();
        this.job = job;
        this.batchNumber = batchNumber;
        this.requestedCount = requestedCount;
        this.createdCount = 0;
        this.status = FlashcardGenerationStatus.PENDING;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    public static FlashcardGenerationBatchEntity create(
            FlashcardGenerationJobEntity job, int batchNumber, int requestedCount) {
        if (job == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard generation batch job is required");
        }
        if (batchNumber <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "Flashcard generation batch number must be greater than zero");
        }
        if (requestedCount <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "Flashcard generation batch requested count must be greater than zero");
        }
        return new FlashcardGenerationBatchEntity(job, batchNumber, requestedCount);
    }

    public void start(OffsetDateTime now) {
        this.status = FlashcardGenerationStatus.RUNNING;
        this.startedAt = now;
        this.updatedAt = OffsetDateTime.now();
    }

    public void complete(int createdCount, OffsetDateTime now) {
        this.createdCount = createdCount;
        this.status = createdCount >= requestedCount
                ? FlashcardGenerationStatus.COMPLETED
                : FlashcardGenerationStatus.PARTIAL_COMPLETED;
        this.finishedAt = now;
        this.updatedAt = OffsetDateTime.now();
    }

    public void fail(String errorMessage, int createdCount, OffsetDateTime now) {
        this.createdCount = createdCount;
        this.status = createdCount > 0 ? FlashcardGenerationStatus.PARTIAL_COMPLETED : FlashcardGenerationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.finishedAt = now;
        this.updatedAt = OffsetDateTime.now();
    }

    public void cancel(String errorMessage, OffsetDateTime now) {
        if (status != FlashcardGenerationStatus.PENDING) {
            return;
        }
        this.status = FlashcardGenerationStatus.CANCELED;
        this.errorMessage = errorMessage;
        this.finishedAt = now;
        this.updatedAt = OffsetDateTime.now();
    }
}
