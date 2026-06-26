package br.com.ottonsam.toothy_planner_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.diet.usecases.FoodNutritionAiClient;
import br.com.ottonsam.toothy_planner_api.diet.usecases.FoodNutritionData;
import br.com.ottonsam.toothy_planner_api.diet.usecases.NutritionValues;
import br.com.ottonsam.toothy_planner_api.report.usecases.WeeklyReportAiClient;
import br.com.ottonsam.toothy_planner_api.user.repositories.ProfileImageStorage;
import br.com.ottonsam.toothy_planner_api.user.repositories.UserActivationCodeRepository;
import br.com.ottonsam.toothy_planner_api.user.repositories.UserRepository;
import br.com.ottonsam.toothy_planner_api.user.usecases.ProfileImageData;
import br.com.ottonsam.toothy_planner_api.user.usecases.ProfileImagePayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class DietIntegrationTests {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final UserActivationCodeRepository activationCodeRepository;
    private final FakeFoodNutritionAiClient foodNutritionAiClient;

    @Autowired
    DietIntegrationTests(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            UserActivationCodeRepository activationCodeRepository,
            FoodNutritionAiClient foodNutritionAiClient) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.activationCodeRepository = activationCodeRepository;
        this.foodNutritionAiClient = (FakeFoodNutritionAiClient) foodNutritionAiClient;
    }

    @BeforeEach
    void resetFakeClient() {
        foodNutritionAiClient.reset();
    }

    @Test
    void managesDefaultAndDailyGoals() throws Exception {
        var userCookie = login("diet-goals@example.com");

        mockMvc.perform(get("/api/v1/diet/goals/default").cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kcal").value(0.00));

        updateDefaultGoal(userCookie, 2200, 160, 250, 70);

        mockMvc.perform(get("/api/v1/diet/goals/daily?date=2026-07-13").cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kcal").value(2200.00))
                .andExpect(jsonPath("$.protein").value(160.00));

        mockMvc.perform(put("/api/v1/diet/goals/daily?date=2026-07-12")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(goal(1800, 150, 180, 60))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kcal").value(1800.00));

        mockMvc.perform(put("/api/v1/diet/goals/daily?date=2026-07-14")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(goal(1200, 90, 120, 40))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kcal").value(1200.00));

        updateDefaultGoal(userCookie, 2100, 155, 240, 65);

        mockMvc.perform(get("/api/v1/diet/goals/daily?date=2026-07-12").cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kcal").value(1800.00));

        mockMvc.perform(get("/api/v1/diet/goals/daily?date=2026-07-13").cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kcal").value(2100.00));

        mockMvc.perform(get("/api/v1/diet/goals/daily?date=2026-07-14").cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kcal").value(2100.00));
    }

    @Test
    void createsEntriesWithDeepSeekOnlyWhenFoodIsNewAndCalculatesMetrics() throws Exception {
        var userCookie = login("diet-entries@example.com");
        updateDefaultGoal(userCookie, 2200, 160, 250, 70);

        var firstEntryId = createEntry(userCookie, "maçã", 2, "PORTIONS", "2026-07-13");

        mockMvc.perform(post("/api/v1/diet/entries")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "foodName", "MAÇÃ",
                                "quantity", 100,
                                "unit", "GRAMS",
                                "entryDate", "2026-07-13"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.food.name").value("MAÇÃ"))
                .andExpect(jsonPath("$.kcal").value(52.00))
                .andExpect(jsonPath("$.protein").value(0.30))
                .andExpect(jsonPath("$.carbohydrate").value(14.00))
                .andExpect(jsonPath("$.fat").value(0.20));

        assertThat(foodNutritionAiClient.calls("MAÇÃ")).isEqualTo(1);

        mockMvc.perform(get("/api/v1/diet/foods?name=maç").cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("MAÇÃ"))
                .andExpect(jsonPath("$[0].portionDescription").value("1 unidade media"));

        mockMvc.perform(get("/api/v1/diet/entries?date=2026-07-13").cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(firstEntryId.toString()))
                .andExpect(jsonPath("$[0].kcal").value(190.00))
                .andExpect(jsonPath("$[1].kcal").value(52.00));

        mockMvc.perform(get("/api/v1/diet/metrics?date=2026-07-13").cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumed.kcal").value(242.00))
                .andExpect(jsonPath("$.consumed.protein").value(1.30))
                .andExpect(jsonPath("$.consumed.carbohydrate").value(64.00))
                .andExpect(jsonPath("$.consumed.fat").value(0.80))
                .andExpect(jsonPath("$.remaining.kcal").value(1958.00))
                .andExpect(jsonPath("$.entries").isArray());
    }

    @Test
    void isolatesFoodsAndEntriesByUser() throws Exception {
        var userCookie = login("diet-owner@example.com");
        var otherCookie = login("diet-other@example.com");
        var entryId = createEntry(userCookie, "ovo", 1, "PORTIONS", "2026-07-13");
        var foods = getJson(userCookie, "/api/v1/diet/foods?name=ovo");
        var foodId = UUID.fromString(foods.get(0).get("id").asText());

        mockMvc.perform(get("/api/v1/diet/foods/{foodId}", foodId).cookie(otherCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Food not found"));

        mockMvc.perform(get("/api/v1/diet/entries/{entryId}", entryId).cookie(otherCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Diet entry not found"));

        mockMvc.perform(delete("/api/v1/diet/entries/{entryId}", entryId).cookie(userCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/diet/entries/{entryId}", entryId).cookie(userCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void editsEntryQuantityAndUnitRecalculatingMacrosWithoutDeepSeek() throws Exception {
        var userCookie = login("diet-entry-edit@example.com");
        var otherCookie = login("diet-entry-edit-other@example.com");
        var entryId = createEntry(userCookie, "ovo", 1, "PORTIONS", "2026-07-13");
        assertThat(foodNutritionAiClient.calls("OVO")).isEqualTo(1);

        mockMvc.perform(put("/api/v1/diet/entries/{entryId}", entryId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("quantity", 100, "unit", "GRAMS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.food.name").value("OVO"))
                .andExpect(jsonPath("$.entryDate").value("2026-07-13"))
                .andExpect(jsonPath("$.quantity").value(100.00))
                .andExpect(jsonPath("$.unit").value("GRAMS"))
                .andExpect(jsonPath("$.kcal").value(143.00))
                .andExpect(jsonPath("$.protein").value(13.00))
                .andExpect(jsonPath("$.carbohydrate").value(1.00))
                .andExpect(jsonPath("$.fat").value(10.00));

        mockMvc.perform(put("/api/v1/diet/entries/{entryId}", entryId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("quantity", 2, "unit", "PORTIONS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(2.00))
                .andExpect(jsonPath("$.unit").value("PORTIONS"))
                .andExpect(jsonPath("$.kcal").value(136.00))
                .andExpect(jsonPath("$.protein").value(12.00))
                .andExpect(jsonPath("$.carbohydrate").value(1.00))
                .andExpect(jsonPath("$.fat").value(10.00));

        assertThat(foodNutritionAiClient.calls("OVO")).isEqualTo(1);

        mockMvc.perform(get("/api/v1/diet/metrics?date=2026-07-13").cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumed.kcal").value(136.00))
                .andExpect(jsonPath("$.entries[0].id").value(entryId.toString()));

        mockMvc.perform(put("/api/v1/diet/entries/{entryId}", entryId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("quantity", 0, "unit", "GRAMS"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Diet entry quantity must be greater than zero"));

        mockMvc.perform(put("/api/v1/diet/entries/{entryId}", entryId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("quantity", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Diet entry unit is required"));

        mockMvc.perform(put("/api/v1/diet/entries/{entryId}", entryId)
                        .cookie(otherCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("quantity", 1, "unit", "GRAMS"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Diet entry not found"));
    }

    @Test
    void rejectsInvalidRequestsAndDoesNotSaveEntryWhenDeepSeekFails() throws Exception {
        var userCookie = login("diet-validation@example.com");

        mockMvc.perform(put("/api/v1/diet/goals/default")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(goal(-1, 100, 100, 50))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Diet goal kcal must be greater than or equal to zero"));

        mockMvc.perform(post("/api/v1/diet/entries")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "foodName", "ovo",
                                "quantity", 0,
                                "unit", "GRAMS",
                                "entryDate", "2026-07-13"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Diet entry quantity must be greater than zero"));

        mockMvc.perform(post("/api/v1/diet/entries")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "foodName", "falha",
                                "quantity", 1,
                                "unit", "PORTIONS",
                                "entryDate", "2026-07-13"))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("DeepSeek nutrition lookup failed"));

        mockMvc.perform(get("/api/v1/diet/foods?name=falha").cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    private void updateDefaultGoal(Cookie accessToken, int kcal, int protein, int carbohydrate, int fat)
            throws Exception {
        mockMvc.perform(put("/api/v1/diet/goals/default")
                        .cookie(accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(goal(kcal, protein, carbohydrate, fat))))
                .andExpect(status().isOk());
    }

    private UUID createEntry(Cookie accessToken, String foodName, int quantity, String unit, String entryDate)
            throws Exception {
        var response = mockMvc.perform(post("/api/v1/diet/entries")
                        .cookie(accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "foodName", foodName,
                                "quantity", quantity,
                                "unit", unit,
                                "entryDate", entryDate))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse();
        return readId(response.getContentAsString());
    }

    private JsonNode getJson(Cookie accessToken, String path) throws Exception {
        var response = mockMvc.perform(get(path).cookie(accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        return objectMapper.readTree(response.getContentAsString());
    }

    private Map<String, Integer> goal(int kcal, int protein, int carbohydrate, int fat) {
        return Map.of("kcal", kcal, "protein", protein, "carbohydrate", carbohydrate, "fat", fat);
    }

    private Cookie login(String email) throws Exception {
        createAndActivateUser(email);
        var response = mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "Strong1!"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        return response.getCookie("access_token");
    }

    private void createAndActivateUser(String email) throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Test User", "email", email, "password", "Strong1!"))))
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
        assertThat(userRepository.findByEmail(email).orElseThrow().isActive()).isTrue();
    }

    private UUID readId(String responseBody) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return UUID.fromString(jsonNode.get("id").asText());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    static class FakeFoodNutritionAiClient implements FoodNutritionAiClient {

        private final Map<String, Integer> calls = new HashMap<>();

        @Override
        public FoodNutritionData lookup(String foodName) {
            calls.merge(foodName, 1, Integer::sum);
            if ("FALHA".equals(foodName)) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "DeepSeek nutrition lookup failed");
            }
            if ("OVO".equals(foodName)) {
                return new FoodNutritionData(
                        "OVO",
                        values("1.43", "0.13", "0.01", "0.10"),
                        "1 ovo medio",
                        values("68.00", "6.00", "0.50", "5.00"));
            }
            return new FoodNutritionData(
                    "MAÇÃ",
                    values("0.52", "0.003", "0.14", "0.002"),
                    "1 unidade media",
                    values("95.00", "0.50", "25.00", "0.30"));
        }

        int calls(String foodName) {
            return calls.getOrDefault(foodName, 0);
        }

        void reset() {
            calls.clear();
        }

        private NutritionValues values(String kcal, String protein, String carbohydrate, String fat) {
            return new NutritionValues(
                    new BigDecimal(kcal), new BigDecimal(protein), new BigDecimal(carbohydrate), new BigDecimal(fat));
        }
    }

    @TestConfiguration
    static class TestStorageConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-07-13T12:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        @Primary
        FoodNutritionAiClient foodNutritionAiClient() {
            return new FakeFoodNutritionAiClient();
        }

        @Bean
        @Primary
        WeeklyReportAiClient weeklyReportAiClient() {
            return prompt -> "# Relatorio Semanal de Desempenho\n\n## Resumo\nRelatorio gerado.";
        }

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
