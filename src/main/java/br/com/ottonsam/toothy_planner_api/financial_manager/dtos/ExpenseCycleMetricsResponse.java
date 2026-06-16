package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseCycleMetricsResponse(
        UUID walletId,
        UUID cycleId,
        int referenceMonth,
        int referenceYear,
        LocalDate startsAt,
        LocalDate endsAt,
        BigDecimal spendingGoal,
        BigDecimal totalSpent,
        BigDecimal remainingAmount,
        BigDecimal remainingDailyAmount,
        BigDecimal installmentTotalFromCurrentCycle,
        BigDecimal recurringMonthlyTotal,
        BigDecimal oneTimeTotal) {}
