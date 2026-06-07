package br.com.ottonsam.toothy_planner_api.goal.dtos;

import br.com.ottonsam.toothy_planner_api.goal.entities.GoalType;

public record GoalRequest(String name, GoalType type, Boolean isComplete) {}
