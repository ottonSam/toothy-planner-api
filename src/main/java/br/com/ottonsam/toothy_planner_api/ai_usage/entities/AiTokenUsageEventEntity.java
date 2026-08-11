package br.com.ottonsam.toothy_planner_api.ai_usage.entities;

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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "ai_token_usage_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiTokenUsageEventEntity {

    @Id
    @NotNull(message = "AI token usage event id is required") private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "AI token usage event user is required") private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monthly_usage_id", nullable = false)
    @NotNull(message = "AI token usage event monthly usage is required") private AiMonthlyTokenUsageEntity monthlyUsage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "AI token usage event feature is required") private AiFeature feature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "AI token usage event status is required") private AiTokenUsageEventStatus status;

    @Column(name = "reserved_tokens", nullable = false)
    @Min(value = 1, message = "AI reserved tokens must be greater than zero") private long reservedTokens;

    @Column(name = "prompt_tokens")
    private Long promptTokens;

    @Column(name = "completion_tokens")
    private Long completionTokens;

    @Column(name = "total_tokens")
    private Long totalTokens;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull(message = "Updated at is required") private OffsetDateTime updatedAt;

    private AiTokenUsageEventEntity(
            UserEntity user,
            AiMonthlyTokenUsageEntity monthlyUsage,
            AiFeature feature,
            long reservedTokens,
            OffsetDateTime now) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.monthlyUsage = monthlyUsage;
        this.feature = feature;
        this.status = AiTokenUsageEventStatus.RESERVED;
        this.reservedTokens = reservedTokens;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static AiTokenUsageEventEntity reserve(
            UserEntity user,
            AiMonthlyTokenUsageEntity monthlyUsage,
            AiFeature feature,
            long reservedTokens,
            OffsetDateTime now) {
        if (user == null || monthlyUsage == null || feature == null || reservedTokens <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AI token usage reservation is invalid");
        }
        return new AiTokenUsageEventEntity(user, monthlyUsage, feature, reservedTokens, now);
    }

    public boolean isReserved() {
        return status == AiTokenUsageEventStatus.RESERVED;
    }

    public void charge(long promptTokens, long completionTokens, long totalTokens, OffsetDateTime now) {
        validateUsage(promptTokens, completionTokens, totalTokens);
        if (!isReserved()) {
            return;
        }
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
        this.status = AiTokenUsageEventStatus.CHARGED;
        this.updatedAt = now;
    }

    public void chargeReserved(OffsetDateTime now) {
        charge(0, reservedTokens, reservedTokens, now);
    }

    public void release(OffsetDateTime now) {
        if (!isReserved()) {
            return;
        }
        this.status = AiTokenUsageEventStatus.RELEASED;
        this.updatedAt = now;
    }

    private static void validateUsage(long promptTokens, long completionTokens, long totalTokens) {
        if (promptTokens < 0 || completionTokens < 0 || totalTokens < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AI token usage cannot be negative");
        }
    }
}
