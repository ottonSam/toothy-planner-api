package br.com.ottonsam.toothy_planner_api.user.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.user.dtos.UpdateMeRequest;
import br.com.ottonsam.toothy_planner_api.user.dtos.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateCurrentUserUseCase {

    private final CurrentUserProvider currentUserProvider;
    private final PasswordEncoder passwordEncoder;
    private final ProfileImageUrlFactory profileImageUrlFactory;

    public UpdateCurrentUserUseCase(
            CurrentUserProvider currentUserProvider,
            PasswordEncoder passwordEncoder,
            ProfileImageUrlFactory profileImageUrlFactory) {
        this.currentUserProvider = currentUserProvider;
        this.passwordEncoder = passwordEncoder;
        this.profileImageUrlFactory = profileImageUrlFactory;
    }

    @Transactional
    public UserResponse execute(UpdateMeRequest request) {
        var user = currentUserProvider.get();
        var encodedPassword = request.password() == null ? null : passwordEncoder.encode(request.password());
        user.updateProfile(request.name(), request.password(), encodedPassword, request.theme());
        return UserResponse.from(user, profileImageUrlFactory.createUrl());
    }
}
