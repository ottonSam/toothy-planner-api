package br.com.ottonsam.toothy_planner_api.activity.repositories;

import br.com.ottonsam.toothy_planner_api.activity.entities.ActivityProgressDayEntity;
import br.com.ottonsam.toothy_planner_api.activity.entities.WeekDay;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityProgressDayRepository extends JpaRepository<ActivityProgressDayEntity, UUID> {

    boolean existsByActivityIdAndDay(UUID activityId, WeekDay day);

    long countByActivityId(UUID activityId);

    List<ActivityProgressDayEntity> findAllByActivityIdOrderByCreatedAtAsc(UUID activityId);
}
