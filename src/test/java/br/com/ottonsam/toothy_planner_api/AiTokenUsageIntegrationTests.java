package br.com.ottonsam.toothy_planner_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.ottonsam.toothy_planner_api.ai_usage.entities.AiFeature;
import br.com.ottonsam.toothy_planner_api.ai_usage.usecases.AiTokenUsageUseCase;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.user.repositories.UserActivationCodeRepository;
import br.com.ottonsam.toothy_planner_api.user.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AiTokenUsageIntegrationTests {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final UserActivationCodeRepository activationCodeRepository;
    private final AiTokenUsageUseCase tokenUsageUseCase;

    @Autowired
    AiTokenUsageIntegrationTests(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            UserActivationCodeRepository activationCodeRepository,
            AiTokenUsageUseCase tokenUsageUseCase) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.activationCodeRepository = activationCodeRepository;
        this.tokenUsageUseCase = tokenUsageUseCase;
    }

    @Test
    void returnsOnlyCurrentAvailabilityPercentageForAuthenticatedUser() throws Exception {
        var accessToken = login("ai-percentage@example.com");

        mockMvc.perform(get("/api/v1/ai-usage/current").cookie(accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingPercentage").value(100.0))
                .andExpect(jsonPath("$.periodEndsAt").exists())
                .andExpect(jsonPath("$.exhausted").value(false))
                .andExpect(jsonPath("$.usedTokens").doesNotExist())
                .andExpect(jsonPath("$.remainingTokens").doesNotExist())
                .andExpect(jsonPath("$.reservedTokens").doesNotExist())
                .andExpect(jsonPath("$.limitTokens").doesNotExist());
    }

    @Test
    void chargesProviderUsageIdempotentlyAndRejectsReservationsAboveTheMonthlyLimit() throws Exception {
        var accessToken = login("ai-charge@example.com");
        var userId = userRepository
                .findByEmail("ai-charge@example.com")
                .orElseThrow()
                .getId();
        var reservationId = tokenUsageUseCase.reserve(userId, AiFeature.WEEKLY_REPORT, "{}", 200000);
        var usage =
                objectMapper.readTree("{\"prompt_tokens\":50000,\"completion_tokens\":75000,\"total_tokens\":125000}");

        tokenUsageUseCase.charge(reservationId, usage);
        tokenUsageUseCase.charge(reservationId, usage);

        mockMvc.perform(get("/api/v1/ai-usage/current").cookie(accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingPercentage").value(75.0))
                .andExpect(jsonPath("$.exhausted").value(false));

        assertThatThrownBy(() ->
                        tokenUsageUseCase.reserve(userId, AiFeature.EXPENSE_CLASSIFICATION, "x".repeat(500000), 1))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getStatus())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void protectsTheCurrentUsageEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/ai-usage/current")).andExpect(status().isForbidden());
    }

    @Test
    void serializesConcurrentReservationsSoTheyCannotExceedTheLimit() throws Exception {
        login("ai-concurrency@example.com");
        var userId = userRepository
                .findByEmail("ai-concurrency@example.com")
                .orElseThrow()
                .getId();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> reserveLargeRequest(userId, start));
            var second = executor.submit(() -> reserveLargeRequest(userId, start));
            start.countDown();

            assertThat(java.util.List.of(first.get(), second.get())).containsExactlyInAnyOrder("RESERVED", "BLOCKED");
        }
    }

    private String reserveLargeRequest(java.util.UUID userId, CountDownLatch start) throws InterruptedException {
        start.await();
        try {
            tokenUsageUseCase.reserve(userId, AiFeature.FLASHCARD_GENERATION, "{}", 300000);
            return "RESERVED";
        } catch (ApiException exception) {
            if (exception.getStatus() == HttpStatus.TOO_MANY_REQUESTS) {
                return "BLOCKED";
            }
            throw exception;
        }
    }

    private Cookie login(String email) throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "AI Usage User", "email", email, "password", "Strong1!"))))
                .andExpect(status().isCreated());
        var user = userRepository.findByEmail(email).orElseThrow();
        var code = activationCodeRepository.findAll().stream()
                .filter(activationCode -> activationCode.getUserId().equals(user.getId()))
                .findFirst()
                .orElseThrow()
                .getCode();
        mockMvc.perform(post("/api/v1/users/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "code", code))))
                .andExpect(status().isOk());
        return mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "Strong1!"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookie("access_token");
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
