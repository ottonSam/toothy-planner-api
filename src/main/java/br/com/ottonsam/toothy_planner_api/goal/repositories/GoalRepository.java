package br.com.ottonsam.toothy_planner_api.goal.repositories;

import br.com.ottonsam.toothy_planner_api.goal.entities.GoalEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<GoalEntity, UUID> {

    List<GoalEntity> findAllByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<GoalEntity> findByIdAndUserId(UUID id, UUID userId);

    List<GoalEntity> findAllByIdInAndUserId(Collection<UUID> ids, UUID userId);
}
