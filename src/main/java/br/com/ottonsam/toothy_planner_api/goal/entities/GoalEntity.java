package br.com.ottonsam.toothy_planner_api.goal.entities;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.user.entities.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "goals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoalEntity {

    @Id
    @NotNull(message = "Goal id is required") private UUID id;

    @Column(nullable = false)
    @NotBlank(message = "Goal name is required") @Size(min = 3, message = "Goal name must have at least 3 characters") private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Goal type is required") private GoalType type;

    @Column(name = "is_complete", nullable = false)
    private boolean complete;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "Goal user is required") private UserEntity user;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull(message = "Updated at is required") private OffsetDateTime updatedAt;

    private GoalEntity(UUID id, String name, GoalType type, boolean complete, UserEntity user) {
        this.id = id;
        this.name = name.trim();
        this.type = type;
        this.complete = complete;
        this.user = user;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    public static GoalEntity create(String name, GoalType type, Boolean complete, UserEntity user) {
        validateName(name);
        validateType(type);
        validateUser(user);
        return new GoalEntity(UUID.randomUUID(), name, type, complete != null && complete, user);
    }

    public void update(String name, GoalType type, Boolean complete) {
        validateName(name);
        validateType(type);
        if (complete == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Goal completion status is required");
        }
        this.name = name.trim();
        this.type = type;
        this.complete = complete;
        this.updatedAt = OffsetDateTime.now();
    }

    public static void validateName(String name) {
        if (name == null || name.trim().length() < 3) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Goal name must have at least 3 characters");
        }
    }

    public static void validateType(GoalType type) {
        if (type == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Goal type is required");
        }
    }

    private static void validateUser(UserEntity user) {
        if (user == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Goal user is required");
        }
    }

    public boolean isComplete() {
        return complete;
    }
}
