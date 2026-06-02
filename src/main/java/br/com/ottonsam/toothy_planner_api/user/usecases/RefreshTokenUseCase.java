package br.com.ottonsam.toothy_planner_api.user.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.AuthTokens;
import br.com.ottonsam.toothy_planner_api.auth.usecases.JwtTokenService;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.user.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenUseCase {

    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;

    public RefreshTokenUseCase(JwtTokenService jwtTokenService, UserRepository userRepository) {
        this.jwtTokenService = jwtTokenService;
        this.userRepository = userRepository;
    }

    public AuthTokens execute(String refreshToken) {
        var userId = jwtTokenService.validate(refreshToken, JwtTokenService.REFRESH_TYPE);
        if (userId == null || !userRepository.existsById(userId)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        return jwtTokenService.createTokens(userId);
    }
}
