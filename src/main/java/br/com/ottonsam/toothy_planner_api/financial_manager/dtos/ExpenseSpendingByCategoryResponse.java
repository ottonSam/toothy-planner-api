package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import java.math.BigDecimal;

public record ExpenseSpendingByCategoryResponse(
        ExpenseCategorySummaryResponse category, BigDecimal totalSpent, BigDecimal percentage) {}
