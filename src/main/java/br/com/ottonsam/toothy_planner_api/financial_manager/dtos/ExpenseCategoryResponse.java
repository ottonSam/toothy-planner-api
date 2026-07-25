package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseCategory;

public record ExpenseCategoryResponse(String key, String name, String color, String icon, String description) {

    public static ExpenseCategoryResponse from(ExpenseCategory category) {
        return new ExpenseCategoryResponse(
                category.getKey(),
                category.getName(),
                category.getColor(),
                category.getIcon(),
                category.getDescription());
    }
}
