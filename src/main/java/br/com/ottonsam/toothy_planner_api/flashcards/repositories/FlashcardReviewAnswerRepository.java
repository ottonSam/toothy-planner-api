package br.com.ottonsam.toothy_planner_api.flashcards.repositories;

import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardReviewAnswerEntity;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardReviewRating;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlashcardReviewAnswerRepository extends JpaRepository<FlashcardReviewAnswerEntity, UUID> {

    long countByUserIdAndAnsweredAtGreaterThanEqual(UUID userId, OffsetDateTime answeredAt);

    long countByUserIdAndRating(UUID userId, FlashcardReviewRating rating);
}
