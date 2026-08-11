package br.com.ottonsam.toothy_planner_api.ai_usage.entities;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.user.entities.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "ai_monthly_token_usage")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiMonthlyTokenUsageEntity {

    @Id
    @NotNull(message = "AI monthly token usage id is required") private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "AI monthly token usage user is required") private UserEntity user;

    @Column(name = "period_start", nullable = false)
    @NotNull(message = "AI monthly token usage period is required") private LocalDate periodStart;

    @Column(name = "limit_tokens", nullable = false)
    @Min(value = 1, message = "AI monthly token limit must be greater than zero") private long limitTokens;

    @Column(name = "used_tokens", nullable = false)
    @Min(value = 0, message = "AI used tokens cannot be negative") private long usedTokens;

    @Column(name = "reserved_tokens", nullable = false)
    @Min(value = 0, message = "AI reserved tokens cannot be negative") private long reservedTokens;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull(message = "Updated at is required") private OffsetDateTime updatedAt;

    private AiMonthlyTokenUsageEntity(UserEntity user, LocalDate periodStart, long limitTokens, OffsetDateTime now) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.periodStart = periodStart;
        this.limitTokens = limitTokens;
        this.usedTokens = 0;
        this.reservedTokens = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static AiMonthlyTokenUsageEntity create(
            UserEntity user, LocalDate periodStart, long limitTokens, OffsetDateTime now) {
        validateCreation(user, periodStart, limitTokens);
        return new AiMonthlyTokenUsageEntity(user, periodStart, limitTokens, now);
    }

    public void reserve(long tokens, OffsetDateTime now) {
        validatePositiveTokens(tokens);
        if (tokens > remainingTokens()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Monthly AI token limit reached");
        }
        reservedTokens += tokens;
        updatedAt = now;
    }

    public void charge(long reserved, long actual, OffsetDateTime now) {
        validatePositiveTokens(reserved);
        validateNonNegativeTokens(actual);
        reservedTokens = Math.max(0, reservedTokens - reserved);
        if (actual > reserved || usedTokens + reservedTokens + actual > limitTokens) {
            usedTokens = limitTokens - reservedTokens;
        } else {
            usedTokens += actual;
        }
        updatedAt = now;
    }

    public void release(long tokens, OffsetDateTime now) {
        validatePositiveTokens(tokens);
        reservedTokens = Math.max(0, reservedTokens - tokens);
        updatedAt = now;
    }

    public long remainingTokens() {
        return Math.max(0, limitTokens - usedTokens - reservedTokens);
    }

    private static void validateCreation(UserEntity user, LocalDate periodStart, long limitTokens) {
        if (user == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AI monthly token usage user is required");
        }
        if (periodStart == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AI monthly token usage period is required");
        }
        validatePositiveTokens(limitTokens);
    }

    private static void validatePositiveTokens(long tokens) {
        if (tokens <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AI token amount must be greater than zero");
        }
    }

    private static void validateNonNegativeTokens(long tokens) {
        if (tokens < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AI token amount cannot be negative");
        }
    }
}
