package br.com.ottonsam.toothy_planner_api.financial_manager.entities;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "recurring_expenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecurringExpenseEntity {

    @Id
    @NotNull(message = "Recurring expense id is required") private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    @NotNull(message = "Recurring expense wallet is required") private ExpenseWalletEntity wallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    @NotNull(message = "Recurring expense category is required") private ExpenseCategoryEntity category;

    @Column(nullable = false)
    @NotBlank(message = "Recurring expense description is required") private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Recurring expense amount is required") @DecimalMin(value = "0.01", message = "Recurring expense amount must be greater than zero") private BigDecimal amount;

    @Column(name = "starts_at", nullable = false)
    @NotNull(message = "Recurring expense start date is required") private LocalDate startsAt;

    @Column(name = "canceled_at")
    private LocalDate canceledAt;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull(message = "Updated at is required") private OffsetDateTime updatedAt;

    private RecurringExpenseEntity(
            UUID id,
            ExpenseWalletEntity wallet,
            ExpenseCategoryEntity category,
            String description,
            BigDecimal amount,
            LocalDate startsAt) {
        this.id = id;
        this.wallet = wallet;
        this.category = category;
        this.description = description.trim();
        this.amount = amount;
        this.startsAt = startsAt;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    public static RecurringExpenseEntity create(
            ExpenseWalletEntity wallet,
            ExpenseCategoryEntity category,
            String description,
            BigDecimal amount,
            LocalDate startsAt) {
        validate(wallet, category, description, amount, startsAt);
        return new RecurringExpenseEntity(UUID.randomUUID(), wallet, category, description, amount, startsAt);
    }

    public void update(ExpenseCategoryEntity category, String description, BigDecimal amount, LocalDate startsAt) {
        validate(wallet, category, description, amount, startsAt);
        this.category = category;
        this.description = description.trim();
        this.amount = amount;
        this.startsAt = startsAt;
        this.updatedAt = OffsetDateTime.now();
    }

    public void cancel(LocalDate canceledAt) {
        if (canceledAt == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Recurring expense cancellation date is required");
        }
        this.canceledAt = canceledAt;
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean isActive() {
        return canceledAt == null;
    }

    private static void validate(
            ExpenseWalletEntity wallet,
            ExpenseCategoryEntity category,
            String description,
            BigDecimal amount,
            LocalDate startsAt) {
        if (wallet == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Recurring expense wallet is required");
        }
        if (category == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Recurring expense category is required");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Recurring expense description is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Recurring expense amount must be greater than zero");
        }
        if (startsAt == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Recurring expense start date is required");
        }
    }
}
