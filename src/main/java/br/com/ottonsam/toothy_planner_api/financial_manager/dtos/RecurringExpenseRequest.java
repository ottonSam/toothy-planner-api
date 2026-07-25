package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseCategory;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringExpenseRequest(
        ExpenseCategory category, String description, BigDecimal amount, LocalDate startsAt) {}
