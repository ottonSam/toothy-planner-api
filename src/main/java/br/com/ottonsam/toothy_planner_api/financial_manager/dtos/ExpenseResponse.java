package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseEntity;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        UUID walletId,
        UUID cycleId,
        ExpenseCategorySummaryResponse category,
        String description,
        BigDecimal amount,
        LocalDate expenseDate,
        ExpenseType type,
        UUID parentExpenseId,
        Integer installmentNumber,
        Integer installmentTotal,
        UUID recurrenceId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static ExpenseResponse from(ExpenseEntity expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getWallet().getId(),
                expense.getCycle().getId(),
                ExpenseCategorySummaryResponse.from(expense.getCategory()),
                expense.getDescription(),
                expense.getAmount(),
                expense.getExpenseDate(),
                expense.getType(),
                expense.getParentExpenseId(),
                expense.getInstallmentNumber(),
                expense.getInstallmentTotal(),
                expense.getRecurrenceId(),
                expense.getCreatedAt(),
                expense.getUpdatedAt());
    }
}
