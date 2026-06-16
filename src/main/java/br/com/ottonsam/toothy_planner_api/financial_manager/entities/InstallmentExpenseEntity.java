package br.com.ottonsam.toothy_planner_api.financial_manager.entities;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
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
@Table(name = "installment_expenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstallmentExpenseEntity {

    @Id
    @NotNull(message = "Installment expense id is required") private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    @NotNull(message = "Installment expense wallet is required") private ExpenseWalletEntity wallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    @NotNull(message = "Installment expense category is required") private ExpenseCategoryEntity category;

    @Column(nullable = false)
    @NotBlank(message = "Installment expense description is required") private String description;

    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "installment_amount", precision = 19, scale = 2)
    private BigDecimal installmentAmount;

    @Column(nullable = false)
    @Min(value = 1, message = "Installments must be greater than zero") private int installments;

    @Column(name = "first_expense_date", nullable = false)
    @NotNull(message = "First expense date is required") private LocalDate firstExpenseDate;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull(message = "Updated at is required") private OffsetDateTime updatedAt;

    private InstallmentExpenseEntity(
            UUID id,
            ExpenseWalletEntity wallet,
            ExpenseCategoryEntity category,
            String description,
            BigDecimal totalAmount,
            BigDecimal installmentAmount,
            int installments,
            LocalDate firstExpenseDate) {
        this.id = id;
        this.wallet = wallet;
        this.category = category;
        this.description = description.trim();
        this.totalAmount = totalAmount;
        this.installmentAmount = installmentAmount;
        this.installments = installments;
        this.firstExpenseDate = firstExpenseDate;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    public static InstallmentExpenseEntity create(
            ExpenseWalletEntity wallet,
            ExpenseCategoryEntity category,
            String description,
            BigDecimal totalAmount,
            BigDecimal installmentAmount,
            Integer installments,
            LocalDate firstExpenseDate) {
        validate(wallet, category, description, totalAmount, installmentAmount, installments, firstExpenseDate);
        return new InstallmentExpenseEntity(
                UUID.randomUUID(),
                wallet,
                category,
                description,
                totalAmount,
                installmentAmount,
                installments,
                firstExpenseDate);
    }

    public void update(
            ExpenseCategoryEntity category,
            String description,
            BigDecimal totalAmount,
            BigDecimal installmentAmount,
            Integer installments,
            LocalDate firstExpenseDate) {
        validate(wallet, category, description, totalAmount, installmentAmount, installments, firstExpenseDate);
        this.category = category;
        this.description = description.trim();
        this.totalAmount = totalAmount;
        this.installmentAmount = installmentAmount;
        this.installments = installments;
        this.firstExpenseDate = firstExpenseDate;
        this.updatedAt = OffsetDateTime.now();
    }

    private static void validate(
            ExpenseWalletEntity wallet,
            ExpenseCategoryEntity category,
            String description,
            BigDecimal totalAmount,
            BigDecimal installmentAmount,
            Integer installments,
            LocalDate firstExpenseDate) {
        if (wallet == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Installment expense wallet is required");
        }
        if (category == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Installment expense category is required");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Installment expense description is required");
        }
        if (installments == null || installments <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Installments must be greater than zero");
        }
        if (firstExpenseDate == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "First expense date is required");
        }
        validateAmountChoice(totalAmount, installmentAmount);
    }

    private static void validateAmountChoice(BigDecimal totalAmount, BigDecimal installmentAmount) {
        var hasTotalAmount = totalAmount != null;
        var hasInstallmentAmount = installmentAmount != null;
        if (hasTotalAmount == hasInstallmentAmount) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "Inform either total amount or installment amount, but not both");
        }
        if (hasTotalAmount && totalAmount.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Total amount must be greater than zero");
        }
        if (hasInstallmentAmount && installmentAmount.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Installment amount must be greater than zero");
        }
    }
}
