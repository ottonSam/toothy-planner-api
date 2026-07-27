package br.com.ottonsam.toothy_planner_api.flashcards.entities;

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
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "flashcard_review_answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlashcardReviewAnswerEntity {

    @Id
    @NotNull(message = "Flashcard review answer id is required") private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "Flashcard review answer user is required") private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    @NotNull(message = "Flashcard review answer card is required") private FlashcardEntity card;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Flashcard review rating is required") private FlashcardReviewRating rating;

    @Column(name = "answered_at", nullable = false)
    @NotNull(message = "Flashcard answer date is required") private OffsetDateTime answeredAt;

    private FlashcardReviewAnswerEntity(FlashcardEntity card, FlashcardReviewRating rating, OffsetDateTime answeredAt) {
        this.id = UUID.randomUUID();
        this.user = card.getUser();
        this.card = card;
        this.rating = rating;
        this.answeredAt = answeredAt;
    }

    public static FlashcardReviewAnswerEntity create(
            FlashcardEntity card, FlashcardReviewRating rating, OffsetDateTime answeredAt) {
        return new FlashcardReviewAnswerEntity(card, rating, answeredAt);
    }
}
