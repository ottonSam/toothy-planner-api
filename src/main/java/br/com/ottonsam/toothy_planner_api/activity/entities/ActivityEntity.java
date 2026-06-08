package br.com.ottonsam.toothy_planner_api.activity.entities;

import br.com.ottonsam.toothy_planner_api.calendar.entities.CalendarEntity;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "activities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityEntity {

    @Id
    @NotNull(message = "Activity id is required") private UUID id;

    @Column(nullable = false)
    @NotBlank(message = "Activity description is required") @Size(min = 3, message = "Activity description must have at least 3 characters") private String description;

    @Column(nullable = false)
    @Min(value = 1, message = "Activity week must be greater than 0") private int week;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calendar_id", nullable = false)
    @NotNull(message = "Activity calendar is required") private CalendarEntity calendar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Activity type is required") private ActivityType type;

    @Column(nullable = false)
    @Min(value = 1, message = "Activity goal must be positive") private int goal;

    @Column(name = "week_starts_at", nullable = false)
    @NotNull(message = "Activity week start date is required") private LocalDate weekStartsAt;

    @Column(name = "week_ends_at", nullable = false)
    @NotNull(message = "Activity week end date is required") private LocalDate weekEndsAt;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull(message = "Updated at is required") private OffsetDateTime updatedAt;

    private ActivityEntity(
            UUID id, String description, int week, CalendarEntity calendar, ActivityType type, int goal) {
        this.id = id;
        this.description = description.trim();
        this.week = week;
        this.calendar = calendar;
        this.type = type;
        this.goal = goal;
        updateWeekRange(calendar, week);
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    public static ActivityEntity create(
            String description, int week, CalendarEntity calendar, ActivityType type, int goal) {
        validateDescription(description);
        validateCalendar(calendar);
        validateWeek(week, calendar.getWeeks());
        validateType(type);
        validateGoal(goal);
        return new ActivityEntity(UUID.randomUUID(), description, week, calendar, type, goal);
    }

    public void update(String description, int week, CalendarEntity calendar, ActivityType type, int goal) {
        validateDescription(description);
        validateCalendar(calendar);
        validateWeek(week, calendar.getWeeks());
        validateType(type);
        validateGoal(goal);
        this.description = description.trim();
        this.week = week;
        this.calendar = calendar;
        this.type = type;
        this.goal = goal;
        updateWeekRange(calendar, week);
        this.updatedAt = OffsetDateTime.now();
    }

    public static void validateDescription(String description) {
        if (description == null || description.trim().length() < 3) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Activity description must have at least 3 characters");
        }
    }

    public static void validateWeek(int week, int calendarWeeks) {
        if (week <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Activity week must be greater than 0");
        }
        if (week > calendarWeeks) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "Activity week must be less than or equal to calendar weeks");
        }
    }

    public static void validateType(ActivityType type) {
        if (type == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Activity type is required");
        }
    }

    public static void validateGoal(int goal) {
        if (goal <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Activity goal must be positive");
        }
    }

    private static void validateCalendar(CalendarEntity calendar) {
        if (calendar == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Activity calendar is required");
        }
    }

    private void updateWeekRange(CalendarEntity calendar, int week) {
        this.weekStartsAt = calendar.getStarts().plusDays((long) (week - 1) * 7);
        this.weekEndsAt = weekStartsAt.plusDays(6);
    }
}
