package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseCategoryEntity;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExpenseCategoryResponse(
        UUID id, String name, String color, String icon, OffsetDateTime createdAt, OffsetDateTime updatedAt) {

    public static ExpenseCategoryResponse from(ExpenseCategoryEntity category) {
        return new ExpenseCategoryResponse(
                category.getId(),
                category.getName(),
                category.getColor(),
                category.getIcon(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}
