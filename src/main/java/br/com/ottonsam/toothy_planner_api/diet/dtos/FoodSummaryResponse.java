package br.com.ottonsam.toothy_planner_api.diet.dtos;

import br.com.ottonsam.toothy_planner_api.diet.entities.FoodEntity;
import java.util.UUID;

public record FoodSummaryResponse(UUID id, String name, String portionDescription) {

    public static FoodSummaryResponse from(FoodEntity food) {
        return new FoodSummaryResponse(food.getId(), food.getName(), food.getPortionDescription());
    }
}
