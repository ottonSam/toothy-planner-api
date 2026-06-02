package br.com.ottonsam.toothy_planner_api.user.dtos;

import br.com.ottonsam.toothy_planner_api.user.entities.UserTheme;

public record UpdateMeRequest(String name, String password, UserTheme theme) {}
