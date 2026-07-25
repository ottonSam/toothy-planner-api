package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseWalletMetricsResponse(
        UUID walletId,
        String description,
        BigDecimal spendingGoal,
        LocalDate startsAt,
        int targetSpendingDay,
        ExpenseCycleResponse currentCycle,
        ExpenseCycleMetricsResponse currentCycleMetrics,
        BigDecimal activeRecurringMonthlyTotal,
        BigDecimal installmentTotalFromCurrentCycle) {}
