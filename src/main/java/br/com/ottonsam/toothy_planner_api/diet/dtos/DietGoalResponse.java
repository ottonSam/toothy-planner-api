package br.com.ottonsam.toothy_planner_api.diet.dtos;

import br.com.ottonsam.toothy_planner_api.diet.entities.DailyDietGoalEntity;
import br.com.ottonsam.toothy_planner_api.diet.entities.DietDefaultGoalEntity;
import java.math.BigDecimal;

public record DietGoalResponse(BigDecimal kcal, BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat) {

    public static DietGoalResponse zero() {
        var zero = BigDecimal.ZERO.setScale(2);
        return new DietGoalResponse(zero, zero, zero, zero);
    }

    public static DietGoalResponse from(DietDefaultGoalEntity goal) {
        return new DietGoalResponse(goal.getKcal(), goal.getProtein(), goal.getCarbohydrate(), goal.getFat());
    }

    public static DietGoalResponse from(DailyDietGoalEntity goal) {
        return new DietGoalResponse(goal.getKcal(), goal.getProtein(), goal.getCarbohydrate(), goal.getFat());
    }
}
