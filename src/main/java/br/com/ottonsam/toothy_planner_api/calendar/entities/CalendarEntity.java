package br.com.ottonsam.toothy_planner_api.calendar.entities;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.goal.entities.GoalEntity;
import br.com.ottonsam.toothy_planner_api.user.entities.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "calendars")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CalendarEntity {

    @Id
    @NotNull(message = "Calendar id is required") private UUID id;

    @Column(nullable = false)
    @NotBlank(message = "Calendar description is required") @Size(min = 3, message = "Calendar description must have at least 3 characters") private String description;

    @Column(nullable = false)
    @Min(value = 1, message = "Calendar weeks must be greater than 0") @Max(value = 52, message = "Calendar weeks must be less than 53") private int weeks;

    @Column(nullable = false)
    @NotNull(message = "Calendar start date is required") private LocalDate starts;

    @ManyToMany
    @JoinTable(
            name = "calendar_goals",
            joinColumns = @JoinColumn(name = "calendar_id"),
            inverseJoinColumns = @JoinColumn(name = "goal_id"))
    private Set<GoalEntity> goals = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "Calendar user is required") private UserEntity user;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull(message = "Updated at is required") private OffsetDateTime updatedAt;

    private CalendarEntity(
            UUID id, String description, int weeks, LocalDate starts, Collection<GoalEntity> goals, UserEntity user) {
        this.id = id;
        this.description = description.trim();
        this.weeks = weeks;
        this.starts = starts;
        this.goals = new HashSet<>(goals);
        this.user = user;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    public static CalendarEntity create(
            String description, int weeks, LocalDate starts, Collection<GoalEntity> goals, UserEntity user) {
        validateDescription(description);
        validateWeeks(weeks);
        validateStarts(starts);
        validateUser(user);
        return new CalendarEntity(
                UUID.randomUUID(), description, weeks, starts, goals == null ? Set.of() : goals, user);
    }

    public void update(String description, int weeks, LocalDate starts, Collection<GoalEntity> goals) {
        validateDescription(description);
        validateWeeks(weeks);
        validateStarts(starts);
        this.description = description.trim();
        this.weeks = weeks;
        this.starts = starts;
        this.goals = new HashSet<>(goals == null ? Set.of() : goals);
        this.updatedAt = OffsetDateTime.now();
    }

    public Set<GoalEntity> getGoals() {
        return Set.copyOf(goals);
    }

    public static void validateDescription(String description) {
        if (description == null || description.trim().length() < 3) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Calendar description must have at least 3 characters");
        }
    }

    public static void validateWeeks(int weeks) {
        if (weeks <= 0 || weeks >= 53) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Calendar weeks must be greater than 0 and less than 53");
        }
    }

    public static void validateStarts(LocalDate starts) {
        if (starts == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Calendar start date is required");
        }
    }

    private static void validateUser(UserEntity user) {
        if (user == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Calendar user is required");
        }
    }
}
