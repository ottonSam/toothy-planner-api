package br.com.ottonsam.toothy_planner_api.flashcards.repositories;

import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlashcardRepository extends JpaRepository<FlashcardEntity, UUID> {

    Page<FlashcardEntity> findAllByDeckIdAndUserId(UUID deckId, UUID userId, Pageable pageable);

    List<FlashcardEntity> findAllByDeckIdOrderByCreatedAtAsc(UUID deckId);

    List<FlashcardEntity> findAllByUserIdAndActiveTrue(UUID userId);

    List<FlashcardEntity> findAllByUserIdAndDeckIdAndActiveTrue(UUID userId, UUID deckId);

    Optional<FlashcardEntity> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByDeckIdAndWordIgnoreCase(UUID deckId, String word);

    boolean existsByDeckIdAndBaseVerbIgnoreCase(UUID deckId, String baseVerb);

    boolean existsByDeckIdAndExpressionIgnoreCase(UUID deckId, String expression);

    long countByUserIdAndActiveTrue(UUID userId);

    long countByUserIdAndLastSeenAtIsNull(UUID userId);

    long countByDeckId(UUID deckId);
}
