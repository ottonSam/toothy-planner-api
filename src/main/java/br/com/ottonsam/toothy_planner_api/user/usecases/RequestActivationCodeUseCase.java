package br.com.ottonsam.toothy_planner_api.user.usecases;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.user.dtos.ActivationCodeRequest;
import br.com.ottonsam.toothy_planner_api.user.entities.UserEntity;
import br.com.ottonsam.toothy_planner_api.user.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestActivationCodeUseCase {

    private final UserRepository userRepository;
    private final ActivationCodeService activationCodeService;

    public RequestActivationCodeUseCase(UserRepository userRepository, ActivationCodeService activationCodeService) {
        this.userRepository = userRepository;
        this.activationCodeService = activationCodeService;
    }

    @Transactional
    public void execute(ActivationCodeRequest request) {
        var user = userRepository
                .findByEmail(UserEntity.normalizeEmail(request.email()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        activationCodeService.createFor(user);
    }
}
