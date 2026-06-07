package br.com.ottonsam.toothy_planner_api.activity.entities;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "activity_progress_times")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityProgressTimeEntity {

    @Id
    @NotNull(message = "Activity progress id is required") private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    @NotNull(message = "Activity is required") private ActivityEntity activity;

    @Column(nullable = false)
    @Min(value = 1, message = "Progress time must be positive") private int minutes;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    private ActivityProgressTimeEntity(ActivityEntity activity, int minutes) {
        this.id = UUID.randomUUID();
        this.activity = activity;
        this.minutes = minutes;
        this.createdAt = OffsetDateTime.now();
    }

    public static ActivityProgressTimeEntity create(ActivityEntity activity, int minutes) {
        if (minutes <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Progress time must be positive");
        }
        return new ActivityProgressTimeEntity(activity, minutes);
    }
}
