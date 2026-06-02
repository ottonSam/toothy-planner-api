package br.com.ottonsam.toothy_planner_api.user.usecases;

import org.springframework.stereotype.Component;

@Component
public class ProfileImageUrlFactory {

    public String createUrl() {
        return "/api/v1/users/image";
    }
}
