package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import br.com.ottonsam.toothy_planner_api.financial_manager.entities.RecurringExpenseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RecurringExpenseResponse(
        UUID id,
        UUID walletId,
        ExpenseCategorySummaryResponse category,
        String description,
        BigDecimal amount,
        LocalDate startsAt,
        LocalDate canceledAt,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static RecurringExpenseResponse from(RecurringExpenseEntity recurringExpense) {
        return new RecurringExpenseResponse(
                recurringExpense.getId(),
                recurringExpense.getWallet().getId(),
                ExpenseCategorySummaryResponse.from(recurringExpense.getCategory()),
                recurringExpense.getDescription(),
                recurringExpense.getAmount(),
                recurringExpense.getStartsAt(),
                recurringExpense.getCanceledAt(),
                recurringExpense.isActive(),
                recurringExpense.getCreatedAt(),
                recurringExpense.getUpdatedAt());
    }
}
