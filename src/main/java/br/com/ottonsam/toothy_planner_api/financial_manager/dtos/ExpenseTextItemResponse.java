package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import java.util.List;

public record ExpenseTextItemResponse(
        String sourceText,
        String type,
        ExpenseResponse expense,
        InstallmentExpenseResponse installmentExpense,
        RecurringExpenseResponse recurringExpense,
        List<ExpenseResponse> generatedExpenses) {

    public ExpenseTextItemResponse {
        generatedExpenses = List.copyOf(generatedExpenses);
    }

    @Override
    public List<ExpenseResponse> generatedExpenses() {
        return List.copyOf(generatedExpenses);
    }
}
