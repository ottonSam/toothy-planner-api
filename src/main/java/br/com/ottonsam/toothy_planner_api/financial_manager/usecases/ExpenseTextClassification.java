package br.com.ottonsam.toothy_planner_api.financial_manager.usecases;

import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseCategory;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseTextClassification(
        ExpenseTextType type,
        ExpenseCategory category,
        String description,
        BigDecimal amount,
        LocalDate expenseDate,
        BigDecimal totalAmount,
        BigDecimal installmentAmount,
        Integer installments,
        LocalDate firstExpenseDate,
        LocalDate startsAt) {}
