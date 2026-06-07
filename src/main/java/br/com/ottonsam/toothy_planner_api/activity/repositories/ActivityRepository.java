package br.com.ottonsam.toothy_planner_api.activity.repositories;

import br.com.ottonsam.toothy_planner_api.activity.entities.ActivityEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<ActivityEntity, UUID> {

    List<ActivityEntity> findAllByCalendarUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<ActivityEntity> findByIdAndCalendarUserId(UUID id, UUID userId);
}
