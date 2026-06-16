package br.com.ottonsam.toothy_planner_api.financial_manager.entities;

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
@Table(name = "expenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseEntity {

    @Id
    @NotNull(message = "Expense id is required") private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    @NotNull(message = "Expense wallet is required") private ExpenseWalletEntity wallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cycle_id", nullable = false)
    @NotNull(message = "Expense cycle is required") private ExpenseCycleEntity cycle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    @NotNull(message = "Expense category is required") private ExpenseCategoryEntity category;

    @Column(nullable = false)
    @NotBlank(message = "Expense description is required") private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Expense amount is required") @DecimalMin(value = "0.01", message = "Expense amount must be greater than zero") private BigDecimal amount;

    @Column(name = "expense_date", nullable = false)
    @NotNull(message = "Expense date is required") private LocalDate expenseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Expense type is required") private ExpenseType type;

    @Column(name = "parent_expense_id")
    private UUID parentExpenseId;

    @Column(name = "installment_number")
    private Integer installmentNumber;

    @Column(name = "installment_total")
    private Integer installmentTotal;

    @Column(name = "recurrence_id")
    private UUID recurrenceId;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull(message = "Updated at is required") private OffsetDateTime updatedAt;

    private ExpenseEntity(
            UUID id,
            ExpenseWalletEntity wallet,
            ExpenseCycleEntity cycle,
            ExpenseCategoryEntity category,
            String description,
            BigDecimal amount,
            LocalDate expenseDate,
            ExpenseType type,
            UUID parentExpenseId,
            Integer installmentNumber,
            Integer installmentTotal,
            UUID recurrenceId) {
        this.id = id;
        this.wallet = wallet;
        this.cycle = cycle;
        this.category = category;
        this.description = description.trim();
        this.amount = amount;
        this.expenseDate = expenseDate;
        this.type = type;
        this.parentExpenseId = parentExpenseId;
        this.installmentNumber = installmentNumber;
        this.installmentTotal = installmentTotal;
        this.recurrenceId = recurrenceId;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    public static ExpenseEntity oneTime(
            ExpenseWalletEntity wallet,
            ExpenseCycleEntity cycle,
            ExpenseCategoryEntity category,
            String description,
            BigDecimal amount,
            LocalDate expenseDate) {
        validateBase(wallet, cycle, category, description, amount, expenseDate);
        return new ExpenseEntity(
                UUID.randomUUID(),
                wallet,
                cycle,
                category,
                description,
                amount,
                expenseDate,
                ExpenseType.ONE_TIME,
                null,
                null,
                null,
                null);
    }

    public static ExpenseEntity installment(
            ExpenseWalletEntity wallet,
            ExpenseCycleEntity cycle,
            ExpenseCategoryEntity category,
            String description,
            BigDecimal amount,
            LocalDate expenseDate,
            UUID parentExpenseId,
            int installmentNumber,
            int installmentTotal) {
        validateBase(wallet, cycle, category, description, amount, expenseDate);
        validateParentExpense(parentExpenseId);
        validateInstallmentPosition(installmentNumber, installmentTotal);
        return new ExpenseEntity(
                UUID.randomUUID(),
                wallet,
                cycle,
                category,
                description,
                amount,
                expenseDate,
                ExpenseType.INSTALLMENT,
                parentExpenseId,
                installmentNumber,
                installmentTotal,
                null);
    }

    public static ExpenseEntity recurring(
            ExpenseWalletEntity wallet,
            ExpenseCycleEntity cycle,
            ExpenseCategoryEntity category,
            String description,
            BigDecimal amount,
            LocalDate expenseDate,
            UUID recurrenceId) {
        validateBase(wallet, cycle, category, description, amount, expenseDate);
        validateRecurrence(recurrenceId);
        return new ExpenseEntity(
                UUID.randomUUID(),
                wallet,
                cycle,
                category,
                description,
                amount,
                expenseDate,
                ExpenseType.RECURRING,
                null,
                null,
                null,
                recurrenceId);
    }

    public void update(
            ExpenseCycleEntity cycle,
            ExpenseCategoryEntity category,
            String description,
            BigDecimal amount,
            LocalDate expenseDate) {
        validateBase(wallet, cycle, category, description, amount, expenseDate);
        this.cycle = cycle;
        this.category = category;
        this.description = description.trim();
        this.amount = amount;
        this.expenseDate = expenseDate;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateGenerated(
            ExpenseCycleEntity cycle,
            ExpenseCategoryEntity category,
            String description,
            BigDecimal amount,
            LocalDate expenseDate) {
        validateBase(wallet, cycle, category, description, amount, expenseDate);
        this.cycle = cycle;
        this.category = category;
        this.description = description.trim();
        this.amount = amount;
        this.expenseDate = expenseDate;
        this.updatedAt = OffsetDateTime.now();
    }

    private static void validateBase(
            ExpenseWalletEntity wallet,
            ExpenseCycleEntity cycle,
            ExpenseCategoryEntity category,
            String description,
            BigDecimal amount,
            LocalDate expenseDate) {
        if (wallet == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Expense wallet is required");
        }
        if (cycle == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Expense cycle is required");
        }
        if (category == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Expense category is required");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Expense description is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Expense amount must be greater than zero");
        }
        if (expenseDate == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Expense date is required");
        }
    }

    private static void validateParentExpense(UUID parentExpenseId) {
        if (parentExpenseId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Parent expense id is required");
        }
    }

    private static void validateRecurrence(UUID recurrenceId) {
        if (recurrenceId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Recurring expense id is required");
        }
    }

    private static void validateInstallmentPosition(int installmentNumber, int installmentTotal) {
        if (installmentNumber <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Installment number must be greater than zero");
        }
        if (installmentTotal <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Installment total must be greater than zero");
        }
        if (installmentNumber > installmentTotal) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Installment number must be less than or equal to total");
        }
    }
}
