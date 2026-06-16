package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseCategoryEntity;
import java.util.UUID;

public record ExpenseCategorySummaryResponse(UUID id, String name, String color, String icon) {

    public static ExpenseCategorySummaryResponse from(ExpenseCategoryEntity category) {
        return new ExpenseCategorySummaryResponse(
                category.getId(), category.getName(), category.getColor(), category.getIcon());
    }
}
