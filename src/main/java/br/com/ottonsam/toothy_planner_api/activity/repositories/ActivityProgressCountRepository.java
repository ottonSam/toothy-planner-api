package br.com.ottonsam.toothy_planner_api.activity.repositories;

import br.com.ottonsam.toothy_planner_api.activity.entities.ActivityProgressCountEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityProgressCountRepository extends JpaRepository<ActivityProgressCountEntity, UUID> {

    @Query(
            "select coalesce(sum(progress.value), 0) from ActivityProgressCountEntity progress where progress.activity.id = :activityId")
    int sumByActivityId(@Param("activityId") UUID activityId);

    List<ActivityProgressCountEntity> findAllByActivityIdOrderByCreatedAtAsc(UUID activityId);
}
