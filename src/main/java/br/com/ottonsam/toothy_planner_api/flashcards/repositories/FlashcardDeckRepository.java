package br.com.ottonsam.toothy_planner_api.flashcards.repositories;

import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardDeckEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlashcardDeckRepository extends JpaRepository<FlashcardDeckEntity, UUID> {

    List<FlashcardDeckEntity> findAllByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<FlashcardDeckEntity> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);
}
