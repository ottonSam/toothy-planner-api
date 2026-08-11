package br.com.ottonsam.toothy_planner_api.diet.usecases;

import java.util.UUID;

public interface FoodNutritionAiClient {

    FoodNutritionData lookup(UUID userId, String foodName);
}
