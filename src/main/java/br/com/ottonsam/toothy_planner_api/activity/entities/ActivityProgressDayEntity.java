package br.com.ottonsam.toothy_planner_api.activity.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activity_progress_days")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityProgressDayEntity {

    @Id
    @NotNull(message = "Activity progress id is required") private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    @NotNull(message = "Activity is required") private ActivityEntity activity;

    @Enumerated(EnumType.STRING)
    @Column(name = "week_day", nullable = false)
    @NotNull(message = "Week day is required") private WeekDay day;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    private ActivityProgressDayEntity(ActivityEntity activity, WeekDay day) {
        this.id = UUID.randomUUID();
        this.activity = activity;
        this.day = day;
        this.createdAt = OffsetDateTime.now();
    }

    public static ActivityProgressDayEntity create(ActivityEntity activity, WeekDay day) {
        return new ActivityProgressDayEntity(activity, day);
    }
}
