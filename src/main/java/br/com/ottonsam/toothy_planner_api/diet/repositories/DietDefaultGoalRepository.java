package br.com.ottonsam.toothy_planner_api.diet.repositories;

import br.com.ottonsam.toothy_planner_api.diet.entities.DietDefaultGoalEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DietDefaultGoalRepository extends JpaRepository<DietDefaultGoalEntity, UUID> {

    Optional<DietDefaultGoalEntity> findByUserId(UUID userId);
}
