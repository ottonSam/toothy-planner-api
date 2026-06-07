package br.com.ottonsam.toothy_planner_api.goal.dtos;

import br.com.ottonsam.toothy_planner_api.goal.entities.GoalEntity;
import br.com.ottonsam.toothy_planner_api.goal.entities.GoalType;
import java.util.UUID;

public record GoalResponse(UUID id, String name, GoalType type, boolean isComplete) {

    public static GoalResponse from(GoalEntity goal) {
        return new GoalResponse(goal.getId(), goal.getName(), goal.getType(), goal.isComplete());
    }
}
