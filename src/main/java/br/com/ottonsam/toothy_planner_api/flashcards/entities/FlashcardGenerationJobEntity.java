package br.com.ottonsam.toothy_planner_api.flashcards.entities;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.user.entities.UserEntity;
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
@Table(name = "flashcard_generation_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlashcardGenerationJobEntity {

    @Id
    @NotNull(message = "Flashcard generation job id is required") private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "Flashcard generation job user is required") private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deck_id", nullable = false)
    @NotNull(message = "Flashcard generation job deck is required") private FlashcardDeckEntity deck;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Flashcard generation job type is required") private FlashcardDeckType type;

    @Column(nullable = false)
    private String context;

    @Column(name = "target_language", nullable = false)
    private String targetLanguage;

    @Column(name = "base_language", nullable = false)
    private String baseLanguage;

    @Column(name = "requested_count", nullable = false)
    @Min(value = 1, message = "Flashcard generation requested count must be greater than zero") private int requestedCount;

    @Column(name = "created_count", nullable = false)
    private int createdCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Flashcard generation job status is required") private FlashcardGenerationStatus status;

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

    private FlashcardGenerationJobEntity(FlashcardDeckEntity deck, int requestedCount) {
        this.id = UUID.randomUUID();
        this.user = deck.getUser();
        this.deck = deck;
        this.type = deck.getType();
        this.context = deck.getContext();
        this.targetLanguage = deck.getTargetLanguage();
        this.baseLanguage = deck.getBaseLanguage();
        this.requestedCount = requestedCount;
        this.createdCount = 0;
        this.status = FlashcardGenerationStatus.PENDING;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    public static FlashcardGenerationJobEntity create(FlashcardDeckEntity deck, Integer requestedCount) {
        if (deck == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard generation job deck is required");
        }
        if (requestedCount == null || requestedCount <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "Flashcard generation requested count must be greater than zero");
        }
        return new FlashcardGenerationJobEntity(deck, requestedCount);
    }

    public void start(OffsetDateTime now) {
        if (status == FlashcardGenerationStatus.PENDING) {
            this.status = FlashcardGenerationStatus.RUNNING;
            this.startedAt = now;
            this.updatedAt = OffsetDateTime.now();
        }
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
}
