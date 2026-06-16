package br.com.ottonsam.toothy_planner_api.diet.repositories;

import br.com.ottonsam.toothy_planner_api.diet.entities.DietEntryEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DietEntryRepository extends JpaRepository<DietEntryEntity, UUID> {

    List<DietEntryEntity> findAllByUserIdAndEntryDateOrderByCreatedAtAsc(UUID userId, LocalDate entryDate);

    Optional<DietEntryEntity> findByIdAndUserId(UUID id, UUID userId);
}
