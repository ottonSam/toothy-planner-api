package br.com.ottonsam.toothy_planner_api.diet.usecases;

public interface FoodNutritionAiClient {

    FoodNutritionData lookup(String foodName);
}
