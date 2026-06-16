package br.com.ottonsam.toothy_planner_api.diet.dtos;

import java.math.BigDecimal;

public record DietGoalRequest(BigDecimal kcal, BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat) {}
