package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseRequest(UUID categoryId, String description, BigDecimal amount, LocalDate expenseDate) {}
