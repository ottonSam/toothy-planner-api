package br.com.ottonsam.toothy_planner_api.calendar.repositories;

import br.com.ottonsam.toothy_planner_api.calendar.entities.CalendarEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarRepository extends JpaRepository<CalendarEntity, UUID> {

    List<CalendarEntity> findAllByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<CalendarEntity> findByIdAndUserId(UUID id, UUID userId);
}
