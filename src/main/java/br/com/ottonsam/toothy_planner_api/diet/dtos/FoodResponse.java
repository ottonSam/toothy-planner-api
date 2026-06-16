package br.com.ottonsam.toothy_planner_api.diet.dtos;

import br.com.ottonsam.toothy_planner_api.diet.entities.FoodEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FoodResponse(
        UUID id,
        String name,
        BigDecimal kcalPerGram,
        BigDecimal proteinPerGram,
        BigDecimal carbohydratePerGram,
        BigDecimal fatPerGram,
        BigDecimal kcalPerPortion,
        BigDecimal proteinPerPortion,
        BigDecimal carbohydratePerPortion,
        BigDecimal fatPerPortion,
        String portionDescription,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static FoodResponse from(FoodEntity food) {
        return new FoodResponse(
                food.getId(),
                food.getName(),
                food.getKcalPerGram(),
                food.getProteinPerGram(),
                food.getCarbohydratePerGram(),
                food.getFatPerGram(),
                food.getKcalPerPortion(),
                food.getProteinPerPortion(),
                food.getCarbohydratePerPortion(),
                food.getFatPerPortion(),
                food.getPortionDescription(),
                food.getCreatedAt(),
                food.getUpdatedAt());
    }
}
