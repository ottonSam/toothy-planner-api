package br.com.ottonsam.toothy_planner_api.ai_usage.repositories;

import br.com.ottonsam.toothy_planner_api.ai_usage.entities.AiTokenUsageEventEntity;
import br.com.ottonsam.toothy_planner_api.ai_usage.entities.AiTokenUsageEventStatus;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiTokenUsageEventRepository extends JpaRepository<AiTokenUsageEventEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from AiTokenUsageEventEntity event where event.id = :id")
    Optional<AiTokenUsageEventEntity> findByIdForUpdate(@Param("id") UUID id);

    List<AiTokenUsageEventEntity> findAllByMonthlyUsageIdAndStatusAndCreatedAtBefore(
            UUID monthlyUsageId, AiTokenUsageEventStatus status, OffsetDateTime createdBefore);
}
