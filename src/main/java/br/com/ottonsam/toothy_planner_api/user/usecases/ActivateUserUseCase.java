package br.com.ottonsam.toothy_planner_api.user.usecases;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.user.dtos.ActivateUserRequest;
import br.com.ottonsam.toothy_planner_api.user.entities.UserEntity;
import br.com.ottonsam.toothy_planner_api.user.repositories.UserActivationCodeRepository;
import br.com.ottonsam.toothy_planner_api.user.repositories.UserRepository;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivateUserUseCase {

    private final UserRepository userRepository;
    private final UserActivationCodeRepository activationCodeRepository;

    public ActivateUserUseCase(UserRepository userRepository, UserActivationCodeRepository activationCodeRepository) {
        this.userRepository = userRepository;
        this.activationCodeRepository = activationCodeRepository;
    }

    @Transactional
    public void execute(ActivateUserRequest request) {
        var user = userRepository
                .findByEmail(UserEntity.normalizeEmail(request.email()))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid activation code"));
        var activationCode = activationCodeRepository
                .findFirstByUserIdAndCodeOrderByExpiresAtDesc(user.getId(), request.code())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid activation code"));
        if (!activationCode.isValid(OffsetDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid activation code");
        }
        user.activate();
        activationCode.markUsed();
    }
}
