package br.com.ottonsam.toothy_planner_api.flashcards.repositories;

import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardTagEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlashcardTagRepository extends JpaRepository<FlashcardTagEntity, UUID> {

    List<FlashcardTagEntity> findAllByUserIdOrderByNameAsc(UUID userId);

    Optional<FlashcardTagEntity> findByUserIdAndNameNormalized(UUID userId, String nameNormalized);
}
