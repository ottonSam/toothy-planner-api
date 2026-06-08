package br.com.ottonsam.toothy_planner_api.report.repositories;

import br.com.ottonsam.toothy_planner_api.report.entities.WeeklyPerformanceReportEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyPerformanceReportRepository extends JpaRepository<WeeklyPerformanceReportEntity, UUID> {

    boolean existsByCalendarIdAndWeek(UUID calendarId, int week);

    Optional<WeeklyPerformanceReportEntity> findByCalendarIdAndWeekAndCalendarUserId(
            UUID calendarId, int week, UUID userId);

    List<WeeklyPerformanceReportEntity> findAllByCalendarIdAndCalendarUserIdOrderByWeekAsc(
            UUID calendarId, UUID userId);

    List<WeeklyPerformanceReportEntity> findTop3ByCalendarIdAndWeekLessThanOrderByWeekDesc(UUID calendarId, int week);
}
