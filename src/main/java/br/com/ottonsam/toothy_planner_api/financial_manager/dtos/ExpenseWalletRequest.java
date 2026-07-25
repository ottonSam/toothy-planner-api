package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseWalletRequest(
        String description, BigDecimal spendingGoal, LocalDate startsAt, Integer targetSpendingDay) {}
