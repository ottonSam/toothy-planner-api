package br.com.ottonsam.toothy_planner_api.report.entities;

import br.com.ottonsam.toothy_planner_api.calendar.entities.CalendarEntity;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.http.HttpStatus;

@Entity
@Table(
        name = "weekly_performance_reports",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_weekly_performance_reports_calendar_week",
                        columnNames = {"calendar_id", "week"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyPerformanceReportEntity {

    @Id
    @NotNull(message = "Weekly performance report id is required") private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calendar_id", nullable = false)
    @NotNull(message = "Calendar is required") private CalendarEntity calendar;

    @Column(nullable = false)
    @Min(value = 1, message = "Week must be greater than 0") private int week;

    @Column(name = "week_starts_at", nullable = false)
    @NotNull(message = "Week start date is required") private LocalDate weekStartsAt;

    @Column(name = "week_ends_at", nullable = false)
    @NotNull(message = "Week end date is required") private LocalDate weekEndsAt;

    @Column(name = "user_feedback", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "User feedback is required") private String userFeedback;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "JSONB")
    @NotBlank(message = "Metrics are required") private String metrics;

    @Column(name = "markdown_report", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Markdown report is required") private String markdownReport;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull(message = "Updated at is required") private OffsetDateTime updatedAt;

    private WeeklyPerformanceReportEntity(
            CalendarEntity calendar,
            int week,
            LocalDate weekStartsAt,
            LocalDate weekEndsAt,
            String userFeedback,
            String metrics,
            String markdownReport) {
        this.id = UUID.randomUUID();
        this.calendar = calendar;
        this.week = week;
        this.weekStartsAt = weekStartsAt;
        this.weekEndsAt = weekEndsAt;
        this.userFeedback = userFeedback.trim();
        this.metrics = metrics;
        this.markdownReport = markdownReport;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    public static WeeklyPerformanceReportEntity create(
            CalendarEntity calendar,
            int week,
            LocalDate weekStartsAt,
            LocalDate weekEndsAt,
            String userFeedback,
            String metrics,
            String markdownReport) {
        validateCalendar(calendar);
        validateWeek(week, calendar.getWeeks());
        validateWeekDates(weekStartsAt, weekEndsAt);
        validateUserFeedback(userFeedback);
        validateMetrics(metrics);
        validateMarkdownReport(markdownReport);
        return new WeeklyPerformanceReportEntity(
                calendar, week, weekStartsAt, weekEndsAt, userFeedback, metrics, markdownReport);
    }

    public static void validateWeek(int week, int calendarWeeks) {
        if (week <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Week must be greater than 0");
        }
        if (week > calendarWeeks) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Week must be less than or equal to calendar weeks");
        }
    }

    public static void validateUserFeedback(String userFeedback) {
        if (userFeedback == null || userFeedback.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "User feedback is required");
        }
    }

    private static void validateCalendar(CalendarEntity calendar) {
        if (calendar == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Calendar is required");
        }
    }

    private static void validateWeekDates(LocalDate weekStartsAt, LocalDate weekEndsAt) {
        if (weekStartsAt == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Week start date is required");
        }
        if (weekEndsAt == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Week end date is required");
        }
    }

    private static void validateMetrics(String metrics) {
        if (metrics == null || metrics.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Metrics are required");
        }
    }

    private static void validateMarkdownReport(String markdownReport) {
        if (markdownReport == null || markdownReport.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Markdown report is required");
        }
    }
}
