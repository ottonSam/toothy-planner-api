package br.com.ottonsam.toothy_planner_api.flashcards.repositories;

import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardGenerationBatchEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlashcardGenerationBatchRepository extends JpaRepository<FlashcardGenerationBatchEntity, UUID> {

    List<FlashcardGenerationBatchEntity> findAllByJobIdOrderByBatchNumberAsc(UUID jobId);
}
