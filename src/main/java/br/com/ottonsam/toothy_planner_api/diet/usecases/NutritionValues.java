package br.com.ottonsam.toothy_planner_api.diet.usecases;

import java.math.BigDecimal;

public record NutritionValues(BigDecimal kcal, BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat) {}
