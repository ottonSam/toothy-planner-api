package br.com.ottonsam.toothy_planner_api.user.dtos;

import br.com.ottonsam.toothy_planner_api.user.entities.UserEntity;
import br.com.ottonsam.toothy_planner_api.user.entities.UserTheme;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email, String profileImage, UserTheme theme, boolean isActive) {

    public static UserResponse from(UserEntity user, String profileImageUrl) {
        return new UserResponse(
                user.getId(), user.getName(), user.getEmail(), profileImageUrl, user.getTheme(), user.isActive());
    }
}
