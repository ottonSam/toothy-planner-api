package br.com.ottonsam.toothy_planner_api.user.usecases;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.user.dtos.CreateUserRequest;
import br.com.ottonsam.toothy_planner_api.user.dtos.UserResponse;
import br.com.ottonsam.toothy_planner_api.user.entities.UserEntity;
import br.com.ottonsam.toothy_planner_api.user.repositories.ProfileImageStorage;
import br.com.ottonsam.toothy_planner_api.user.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileImageValidator profileImageValidator;
    private final ProfileImageStorage profileImageStorage;
    private final ProfileImageUrlFactory profileImageUrlFactory;
    private final ActivationCodeService activationCodeService;

    public CreateUserUseCase(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ProfileImageValidator profileImageValidator,
            ProfileImageStorage profileImageStorage,
            ProfileImageUrlFactory profileImageUrlFactory,
            ActivationCodeService activationCodeService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.profileImageValidator = profileImageValidator;
        this.profileImageStorage = profileImageStorage;
        this.profileImageUrlFactory = profileImageUrlFactory;
        this.activationCodeService = activationCodeService;
    }

    @Transactional
    public UserResponse execute(CreateUserRequest request) {
        var email = UserEntity.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already exists");
        }

        var user = UserEntity.create(
                request.name(),
                email,
                request.password(),
                passwordEncoder.encode(request.password()),
                null,
                request.theme());
        userRepository.save(user);

        if (request.profileImage() != null) {
            var image = profileImageValidator.validate(request.profileImage());
            user.updateProfileImage(profileImageStorage.store(user.getId(), image));
        }

        activationCodeService.createFor(user);
        return UserResponse.from(user, profileImageUrlFactory.createUrl());
    }
}
