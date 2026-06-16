package br.com.ottonsam.toothy_planner_api.financial_manager.entities;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
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
@Table(name = "expense_cycles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseCycleEntity {

    @Id
    @NotNull(message = "Cycle id is required") private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    @NotNull(message = "Cycle wallet is required") private ExpenseWalletEntity wallet;

    @Column(name = "reference_month", nullable = false)
    @Min(value = 1, message = "Cycle reference month must be between 1 and 12") @Max(value = 12, message = "Cycle reference month must be between 1 and 12") private int referenceMonth;

    @Column(name = "reference_year", nullable = false)
    @Min(value = 1, message = "Cycle reference year is required") private int referenceYear;

    @Column(name = "starts_at", nullable = false)
    @NotNull(message = "Cycle start date is required") private LocalDate startsAt;

    @Column(name = "ends_at", nullable = false)
    @NotNull(message = "Cycle end date is required") private LocalDate endsAt;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull(message = "Updated at is required") private OffsetDateTime updatedAt;

    private ExpenseCycleEntity(
            UUID id,
            ExpenseWalletEntity wallet,
            int referenceMonth,
            int referenceYear,
            LocalDate startsAt,
            LocalDate endsAt) {
        this.id = id;
        this.wallet = wallet;
        this.referenceMonth = referenceMonth;
        this.referenceYear = referenceYear;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    public static ExpenseCycleEntity create(
            ExpenseWalletEntity wallet, int referenceMonth, int referenceYear, LocalDate startsAt, LocalDate endsAt) {
        validateWallet(wallet);
        validateReference(referenceMonth, referenceYear);
        validatePeriod(startsAt, endsAt);
        return new ExpenseCycleEntity(UUID.randomUUID(), wallet, referenceMonth, referenceYear, startsAt, endsAt);
    }

    private static void validateWallet(ExpenseWalletEntity wallet) {
        if (wallet == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cycle wallet is required");
        }
    }

    private static void validateReference(int referenceMonth, int referenceYear) {
        if (referenceMonth < 1 || referenceMonth > 12) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cycle reference month must be between 1 and 12");
        }
        if (referenceYear < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cycle reference year is required");
        }
    }

    private static void validatePeriod(LocalDate startsAt, LocalDate endsAt) {
        if (startsAt == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cycle start date is required");
        }
        if (endsAt == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cycle end date is required");
        }
        if (startsAt.isAfter(endsAt)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cycle start date must be before or equal to end date");
        }
    }
}
