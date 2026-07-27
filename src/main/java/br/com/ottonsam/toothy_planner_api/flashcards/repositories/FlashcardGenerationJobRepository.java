package br.com.ottonsam.toothy_planner_api.flashcards.repositories;

import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardGenerationJobEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlashcardGenerationJobRepository extends JpaRepository<FlashcardGenerationJobEntity, UUID> {

    Optional<FlashcardGenerationJobEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<FlashcardGenerationJobEntity> findFirstByDeckIdAndUserIdOrderByCreatedAtDesc(UUID deckId, UUID userId);
}
