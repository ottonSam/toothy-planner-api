package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import java.math.BigDecimal;

public record ExpenseWalletRequest(String description, BigDecimal spendingGoal, Integer cycleEndDay) {}
