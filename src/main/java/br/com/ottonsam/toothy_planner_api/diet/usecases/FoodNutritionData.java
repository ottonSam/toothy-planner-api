package br.com.ottonsam.toothy_planner_api.diet.usecases;

public record FoodNutritionData(
        String name, NutritionValues perGram, String portionDescription, NutritionValues portion) {}
