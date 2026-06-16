package br.com.ottonsam.toothy_planner_api.financial_manager.entities;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.user.entities.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "expense_wallets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseWalletEntity {

    @Id
    @NotNull(message = "Wallet id is required") private UUID id;

    @Column(nullable = false)
    @NotBlank(message = "Wallet description is required") private String description;

    @Column(name = "spending_goal", nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Wallet spending goal is required") @DecimalMin(value = "0.01", message = "Wallet spending goal must be greater than zero") private BigDecimal spendingGoal;

    @Column(name = "cycle_end_day", nullable = false)
    @Min(value = 1, message = "Wallet cycle end day must be between 1 and 31") @Max(value = 31, message = "Wallet cycle end day must be between 1 and 31") private int cycleEndDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "Wallet user is required") private UserEntity user;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull(message = "Updated at is required") private OffsetDateTime updatedAt;

    private ExpenseWalletEntity(
            UUID id, String description, BigDecimal spendingGoal, int cycleEndDay, UserEntity user) {
        this.id = id;
        this.description = description.trim();
        this.spendingGoal = spendingGoal;
        this.cycleEndDay = cycleEndDay;
        this.user = user;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    public static ExpenseWalletEntity create(
            String description, BigDecimal spendingGoal, Integer cycleEndDay, UserEntity user) {
        validateDescription(description);
        validateSpendingGoal(spendingGoal);
        validateCycleEndDay(cycleEndDay);
        validateUser(user);
        return new ExpenseWalletEntity(UUID.randomUUID(), description, spendingGoal, cycleEndDay, user);
    }

    public void update(String description, BigDecimal spendingGoal, Integer cycleEndDay) {
        validateDescription(description);
        validateSpendingGoal(spendingGoal);
        validateCycleEndDay(cycleEndDay);
        this.description = description.trim();
        this.spendingGoal = spendingGoal;
        this.cycleEndDay = cycleEndDay;
        this.updatedAt = OffsetDateTime.now();
    }

    public static void validateDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Wallet description is required");
        }
    }

    public static void validateSpendingGoal(BigDecimal spendingGoal) {
        if (spendingGoal == null || spendingGoal.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Wallet spending goal must be greater than zero");
        }
    }

    public static void validateCycleEndDay(Integer cycleEndDay) {
        if (cycleEndDay == null || cycleEndDay < 1 || cycleEndDay > 31) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Wallet cycle end day must be between 1 and 31");
        }
    }

    private static void validateUser(UserEntity user) {
        if (user == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Wallet user is required");
        }
    }
}
