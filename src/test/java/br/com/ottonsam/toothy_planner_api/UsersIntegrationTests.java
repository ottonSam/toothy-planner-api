package br.com.ottonsam.toothy_planner_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.ottonsam.toothy_planner_api.user.repositories.ProfileImageStorage;
import br.com.ottonsam.toothy_planner_api.user.repositories.UserActivationCodeRepository;
import br.com.ottonsam.toothy_planner_api.user.repositories.UserRepository;
import br.com.ottonsam.toothy_planner_api.user.usecases.ProfileImageData;
import br.com.ottonsam.toothy_planner_api.user.usecases.ProfileImagePayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class UsersIntegrationTests {

    private static final String PNG_BASE64 =
            Base64.getEncoder().encodeToString(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final UserActivationCodeRepository activationCodeRepository;

    @Autowired
    UsersIntegrationTests(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            UserActivationCodeRepository activationCodeRepository) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.activationCodeRepository = activationCodeRepository;
    }

    @Test
    void createsUserInactive() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "John Doe",
                                "email", "john@example.com",
                                "password", "Strong1!",
                                "theme", "DARK"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.theme").value("DARK"))
                .andExpect(jsonPath("$.isActive").value(false))
                .andExpect(jsonPath("$.profileImage").value("/api/v1/users/image"));

        var user = userRepository.findByEmail("john@example.com").orElseThrow();
        assertThat(user.isActive()).isFalse();
        assertThat(user.getPassword()).isNotEqualTo("Strong1!");
    }

    @Test
    void rejectsInvalidCreateRequests() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "J", "email", "bad@example.com", "password", "Strong1!"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Name must have at least 2 characters"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Jane", "email", "invalid", "password", "Strong1!"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email must be valid"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Jane", "email", "jane@example.com", "password", "weak"))))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Password must have at least 8 characters, one uppercase letter, one lowercase letter, one number and one special character"));
    }

    @Test
    void rejectsDuplicateEmailAndInvalidImage() throws Exception {
        createUser("duplicate@example.com");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Duplicate",
                                "email", "duplicate@example.com",
                                "password", "Strong1!"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Image User",
                                "email", "image-invalid@example.com",
                                "password", "Strong1!",
                                "profileImage", "not-base64"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Profile image must be a valid base64 image"));
    }

    @Test
    void inactiveLoginSendsActivationCodeOnlyWhenCredentialsAreCorrect() throws Exception {
        createUser("inactive@example.com");

        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "inactive@example.com", "password", "Strong1!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account activation required. A new activation code was sent."))
                .andExpect(cookie().doesNotExist("access_token"))
                .andExpect(cookie().doesNotExist("refresh_token"));

        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "inactive@example.com", "password", "Wrong1!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void activatesUserAndAuthenticatesWithCookies() throws Exception {
        createAndActivateUser("active@example.com");

        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "active@example.com", "password", "Strong1!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Authenticated"))
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"));
    }

    @Test
    void refreshesTokensAndProtectsPrivateRoutes() throws Exception {
        var cookies = login("private@example.com");

        mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/users/me").cookie(cookies.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("private@example.com"))
                .andExpect(jsonPath("$.profileImage").value("/api/v1/users/image"));

        mockMvc.perform(get("/api/v1/users/refresh").cookie(cookies.refreshToken()))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"));
    }

    @Test
    void logoutClearsAuthenticationCookies() throws Exception {
        var cookies = login("logout@example.com");

        mockMvc.perform(post("/api/v1/users/logout").cookie(cookies.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"))
                .andExpect(cookie().value("access_token", ""))
                .andExpect(cookie().maxAge("access_token", 0))
                .andExpect(cookie().value("refresh_token", ""))
                .andExpect(cookie().maxAge("refresh_token", 0));
    }

    @Test
    void updatesCurrentUserAndProfileImage() throws Exception {
        var cookies = login("update@example.com");

        mockMvc.perform(put("/api/v1/users/me")
                        .cookie(cookies.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Updated User", "password", "NewStrong1!", "theme", "DARK"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated User"))
                .andExpect(jsonPath("$.theme").value("DARK"));

        mockMvc.perform(put("/api/v1/users/image")
                        .cookie(cookies.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("image", PNG_BASE64))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile image updated"));

        mockMvc.perform(get("/api/v1/users/image").cookie(cookies.accessToken()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"));

        mockMvc.perform(put("/api/v1/users/image")
                        .cookie(cookies.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"image\":null}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/image").cookie(cookies.accessToken()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/svg+xml"));
    }

    private void createUser(String email) throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Test User", "email", email, "password", "Strong1!"))))
                .andExpect(status().isCreated());
    }

    private void createAndActivateUser(String email) throws Exception {
        createUser(email);
        var user = userRepository.findByEmail(email).orElseThrow();
        var code = activationCodeRepository.findAll().stream()
                .filter(activationCode -> activationCode.getUserId().equals(user.getId()))
                .findFirst()
                .orElseThrow()
                .getCode();

        mockMvc.perform(post("/api/v1/users/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "code", code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User activated"));
    }

    private AuthCookies login(String email) throws Exception {
        createAndActivateUser(email);
        var response = mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "Strong1!"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        return new AuthCookies(response.getCookie("access_token"), response.getCookie("refresh_token"));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private record AuthCookies(Cookie accessToken, Cookie refreshToken) {}

    @TestConfiguration
    static class TestStorageConfiguration {

        @Bean
        @Primary
        ProfileImageStorage profileImageStorage() {
            return new ProfileImageStorage() {
                private final HashMap<String, ProfileImageData> images = new HashMap<>();

                @Override
                public String store(UUID userId, ProfileImagePayload image) {
                    var key = "users/%s/profile-image/test.%s".formatted(userId, image.extension());
                    images.put(key, new ProfileImageData(image.content(), image.contentType()));
                    return key;
                }

                @Override
                public Optional<ProfileImageData> load(String key) {
                    return Optional.ofNullable(images.get(key));
                }

                @Override
                public void delete(String key) {
                    images.remove(key);
                }
            };
        }
    }
}
