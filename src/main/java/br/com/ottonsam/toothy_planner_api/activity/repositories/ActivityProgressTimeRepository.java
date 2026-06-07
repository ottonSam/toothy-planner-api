package br.com.ottonsam.toothy_planner_api.activity.repositories;

import br.com.ottonsam.toothy_planner_api.activity.entities.ActivityProgressTimeEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityProgressTimeRepository extends JpaRepository<ActivityProgressTimeEntity, UUID> {

    @Query(
            "select coalesce(sum(progress.minutes), 0) from ActivityProgressTimeEntity progress where progress.activity.id = :activityId")
    int sumByActivityId(@Param("activityId") UUID activityId);
}
