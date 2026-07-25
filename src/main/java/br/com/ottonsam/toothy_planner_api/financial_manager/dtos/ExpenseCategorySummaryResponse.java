package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseCategory;

public record ExpenseCategorySummaryResponse(String key, String name, String color, String icon) {

    public static ExpenseCategorySummaryResponse from(ExpenseCategory category) {
        return new ExpenseCategorySummaryResponse(
                category.getKey(), category.getName(), category.getColor(), category.getIcon());
    }
}
