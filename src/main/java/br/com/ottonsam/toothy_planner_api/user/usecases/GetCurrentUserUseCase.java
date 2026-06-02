package br.com.ottonsam.toothy_planner_api.user.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.user.dtos.UserResponse;
import org.springframework.stereotype.Service;

@Service
public class GetCurrentUserUseCase {

    private final CurrentUserProvider currentUserProvider;
    private final ProfileImageUrlFactory profileImageUrlFactory;

    public GetCurrentUserUseCase(
            CurrentUserProvider currentUserProvider, ProfileImageUrlFactory profileImageUrlFactory) {
        this.currentUserProvider = currentUserProvider;
        this.profileImageUrlFactory = profileImageUrlFactory;
    }

    public UserResponse execute() {
        return UserResponse.from(currentUserProvider.get(), profileImageUrlFactory.createUrl());
    }
}
