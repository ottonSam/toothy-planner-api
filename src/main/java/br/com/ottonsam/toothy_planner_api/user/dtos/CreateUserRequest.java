package br.com.ottonsam.toothy_planner_api.user.dtos;

import br.com.ottonsam.toothy_planner_api.user.entities.UserTheme;

public record CreateUserRequest(String name, String email, String password, String profileImage, UserTheme theme) {}
