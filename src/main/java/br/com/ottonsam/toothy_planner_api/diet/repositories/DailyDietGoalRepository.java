package br.com.ottonsam.toothy_planner_api.diet.repositories;

import br.com.ottonsam.toothy_planner_api.diet.entities.DailyDietGoalEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyDietGoalRepository extends JpaRepository<DailyDietGoalEntity, UUID> {

    Optional<DailyDietGoalEntity> findByUserIdAndGoalDate(UUID userId, LocalDate goalDate);

    List<DailyDietGoalEntity> findAllByUserIdAndGoalDateGreaterThanEqual(UUID userId, LocalDate goalDate);
}
