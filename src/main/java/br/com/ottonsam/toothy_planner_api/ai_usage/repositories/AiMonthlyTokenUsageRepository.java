package br.com.ottonsam.toothy_planner_api.ai_usage.repositories;

import br.com.ottonsam.toothy_planner_api.ai_usage.entities.AiMonthlyTokenUsageEntity;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiMonthlyTokenUsageRepository extends JpaRepository<AiMonthlyTokenUsageEntity, UUID> {

    Optional<AiMonthlyTokenUsageEntity> findByUserIdAndPeriodStart(UUID userId, LocalDate periodStart);
}
