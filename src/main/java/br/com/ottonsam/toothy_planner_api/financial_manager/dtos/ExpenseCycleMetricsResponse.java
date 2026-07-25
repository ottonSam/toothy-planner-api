package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExpenseCycleMetricsResponse(
        UUID walletId,
        UUID cycleId,
        int referenceMonth,
        int referenceYear,
        LocalDate startsAt,
        LocalDate endsAt,
        LocalDate targetSpendingDate,
        BigDecimal spendingGoal,
        BigDecimal totalSpent,
        BigDecimal remainingAmount,
        BigDecimal remainingDailyAmount,
        BigDecimal spentUntilTargetDate,
        BigDecimal spentAfterTargetDate,
        BigDecimal installmentTotalFromCurrentCycle,
        BigDecimal recurringMonthlyTotal,
        BigDecimal oneTimeTotal,
        List<ExpenseSpendingByCategoryResponse> spendingByCategory) {

    public ExpenseCycleMetricsResponse {
        spendingByCategory = List.copyOf(spendingByCategory);
    }

    @Override
    public List<ExpenseSpendingByCategoryResponse> spendingByCategory() {
        return List.copyOf(spendingByCategory);
    }
}
