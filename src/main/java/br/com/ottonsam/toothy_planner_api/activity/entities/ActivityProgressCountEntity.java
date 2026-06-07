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
@Table(name = "activity_progress_counts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityProgressCountEntity {

    @Id
    @NotNull(message = "Activity progress id is required") private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    @NotNull(message = "Activity is required") private ActivityEntity activity;

    @Column(name = "progress_value", nullable = false)
    @Min(value = 1, message = "Progress value must be positive") private int value;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    private ActivityProgressCountEntity(ActivityEntity activity, int value) {
        this.id = UUID.randomUUID();
        this.activity = activity;
        this.value = value;
        this.createdAt = OffsetDateTime.now();
    }

    public static ActivityProgressCountEntity create(ActivityEntity activity, int value) {
        if (value <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Progress value must be positive");
        }
        return new ActivityProgressCountEntity(activity, value);
    }
}
