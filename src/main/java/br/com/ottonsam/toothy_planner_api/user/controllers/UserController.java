package br.com.ottonsam.toothy_planner_api.user.controllers;

import br.com.ottonsam.toothy_planner_api.auth.usecases.AuthCookieFactory;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.user.dtos.ActivateUserRequest;
import br.com.ottonsam.toothy_planner_api.user.dtos.ActivationCodeRequest;
import br.com.ottonsam.toothy_planner_api.user.dtos.CreateUserRequest;
import br.com.ottonsam.toothy_planner_api.user.dtos.LoginRequest;
import br.com.ottonsam.toothy_planner_api.user.dtos.MessageResponse;
import br.com.ottonsam.toothy_planner_api.user.dtos.UpdateImageRequest;
import br.com.ottonsam.toothy_planner_api.user.dtos.UpdateMeRequest;
import br.com.ottonsam.toothy_planner_api.user.dtos.UserResponse;
import br.com.ottonsam.toothy_planner_api.user.usecases.ActivateUserUseCase;
import br.com.ottonsam.toothy_planner_api.user.usecases.CreateUserUseCase;
import br.com.ottonsam.toothy_planner_api.user.usecases.GetCurrentUserUseCase;
import br.com.ottonsam.toothy_planner_api.user.usecases.GetProfileImageUseCase;
import br.com.ottonsam.toothy_planner_api.user.usecases.LoginUseCase;
import br.com.ottonsam.toothy_planner_api.user.usecases.RefreshTokenUseCase;
import br.com.ottonsam.toothy_planner_api.user.usecases.RequestActivationCodeUseCase;
import br.com.ottonsam.toothy_planner_api.user.usecases.UpdateCurrentUserUseCase;
import br.com.ottonsam.toothy_planner_api.user.usecases.UpdateProfileImageUseCase;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final UpdateCurrentUserUseCase updateCurrentUserUseCase;
    private final GetProfileImageUseCase getProfileImageUseCase;
    private final UpdateProfileImageUseCase updateProfileImageUseCase;
    private final RequestActivationCodeUseCase requestActivationCodeUseCase;
    private final ActivateUserUseCase activateUserUseCase;
    private final AuthCookieFactory authCookieFactory;

    public UserController(
            CreateUserUseCase createUserUseCase,
            LoginUseCase loginUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            GetCurrentUserUseCase getCurrentUserUseCase,
            UpdateCurrentUserUseCase updateCurrentUserUseCase,
            GetProfileImageUseCase getProfileImageUseCase,
            UpdateProfileImageUseCase updateProfileImageUseCase,
            RequestActivationCodeUseCase requestActivationCodeUseCase,
            ActivateUserUseCase activateUserUseCase,
            AuthCookieFactory authCookieFactory) {
        this.createUserUseCase = createUserUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.updateCurrentUserUseCase = updateCurrentUserUseCase;
        this.getProfileImageUseCase = getProfileImageUseCase;
        this.updateProfileImageUseCase = updateProfileImageUseCase;
        this.requestActivationCodeUseCase = requestActivationCodeUseCase;
        this.activateUserUseCase = activateUserUseCase;
        this.authCookieFactory = authCookieFactory;
    }

    @PostMapping
    ResponseEntity<UserResponse> create(@RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createUserUseCase.execute(request));
    }

    @PostMapping("/login")
    ResponseEntity<MessageResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        var result = loginUseCase.execute(request);
        if (result.authenticated()) {
            authCookieFactory.addAuthCookies(response, result.tokens());
        }
        return ResponseEntity.ok(new MessageResponse(result.message()));
    }

    @GetMapping("/refresh")
    ResponseEntity<MessageResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        authCookieFactory.addAuthCookies(response, refreshTokenUseCase.execute(refreshToken));
        return ResponseEntity.ok(new MessageResponse("Authenticated"));
    }

    @GetMapping("/me")
    UserResponse me() {
        return getCurrentUserUseCase.execute();
    }

    @PutMapping("/me")
    UserResponse updateMe(@RequestBody UpdateMeRequest request) {
        return updateCurrentUserUseCase.execute(request);
    }

    @GetMapping("/image")
    ResponseEntity<byte[]> image() {
        var image = getProfileImageUseCase.execute();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(image.content());
    }

    @PutMapping("/image")
    ResponseEntity<MessageResponse> updateImage(@RequestBody UpdateImageRequest request) {
        updateProfileImageUseCase.execute(request.image());
        return ResponseEntity.ok(new MessageResponse("Profile image updated"));
    }

    @PostMapping("/activation-code")
    ResponseEntity<MessageResponse> requestActivationCode(@RequestBody ActivationCodeRequest request) {
        requestActivationCodeUseCase.execute(request);
        return ResponseEntity.ok(new MessageResponse("Activation code sent"));
    }

    @PostMapping("/activate")
    ResponseEntity<MessageResponse> activate(@RequestBody ActivateUserRequest request) {
        activateUserUseCase.execute(request);
        return ResponseEntity.ok(new MessageResponse("User activated"));
    }
}
