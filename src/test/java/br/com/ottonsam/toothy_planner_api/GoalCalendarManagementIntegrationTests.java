package br.com.ottonsam.toothy_planner_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.ottonsam.toothy_planner_api.report.usecases.WeeklyReportAiClient;
import br.com.ottonsam.toothy_planner_api.user.repositories.ProfileImageStorage;
import br.com.ottonsam.toothy_planner_api.user.repositories.UserActivationCodeRepository;
import br.com.ottonsam.toothy_planner_api.user.repositories.UserRepository;
import br.com.ottonsam.toothy_planner_api.user.usecases.ProfileImageData;
import br.com.ottonsam.toothy_planner_api.user.usecases.ProfileImagePayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class GoalCalendarManagementIntegrationTests {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final UserActivationCodeRepository activationCodeRepository;

    @Autowired
    GoalCalendarManagementIntegrationTests(
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
    void managesGoalsOnlyForAuthenticatedUser() throws Exception {
        var userCookie = login("goal-user@example.com");
        var otherCookie = login("goal-other@example.com");

        var goalId = createGoal(userCookie, "Finish roadmap", "LONG_TERM");

        mockMvc.perform(get("/api/v1/goals/types").cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value("Longo prazo"))
                .andExpect(jsonPath("$[0].value").value("LONG_TERM"));

        mockMvc.perform(get("/api/v1/goals").cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(goalId.toString()));

        mockMvc.perform(get("/api/v1/goals/{id}", goalId).cookie(otherCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Goal not found"));

        mockMvc.perform(put("/api/v1/goals/{id}", goalId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Finish plan", "type", "MEDIUM_TERM", "isComplete", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("MEDIUM_TERM"))
                .andExpect(jsonPath("$.isComplete").value(true));

        mockMvc.perform(delete("/api/v1/goals/{id}", goalId).cookie(userCookie)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/goals/{id}", goalId).cookie(userCookie)).andExpect(status().isNotFound());
    }

    @Test
    void managesCalendarsAndRejectsGoalsFromAnotherUser() throws Exception {
        var userCookie = login("calendar-user@example.com");
        var otherCookie = login("calendar-other@example.com");
        var userGoalId = createGoal(userCookie, "Weekly planning", "CALENDAR");
        var otherGoalId = createGoal(otherCookie, "Private planning", "CALENDAR");

        var calendarId = createCalendar(userCookie, "First quarter", 12, userGoalId);

        mockMvc.perform(get("/api/v1/calendars/{id}", calendarId).cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("First quarter"))
                .andExpect(jsonPath("$.weekStartsOn").value("THURSDAY"))
                .andExpect(jsonPath("$.weekEndsOn").value("WEDNESDAY"))
                .andExpect(jsonPath("$.goalIds[0]").value(userGoalId.toString()));

        mockMvc.perform(post("/api/v1/calendars")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "description",
                                "Invalid goals",
                                "weeks",
                                4,
                                "starts",
                                "2026-01-01",
                                "goalIds",
                                java.util.List.of(otherGoalId)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("All goals must belong to the authenticated user"));

        mockMvc.perform(get("/api/v1/calendars/{id}", calendarId).cookie(otherCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Calendar not found"));

        mockMvc.perform(put("/api/v1/calendars/{id}", calendarId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "description",
                                "Updated calendar",
                                "weeks",
                                8,
                                "starts",
                                "2026-02-01",
                                "goalIds",
                                java.util.List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeks").value(8))
                .andExpect(jsonPath("$.weekStartsOn").value("SUNDAY"))
                .andExpect(jsonPath("$.weekEndsOn").value("SATURDAY"))
                .andExpect(jsonPath("$.goalIds").isEmpty());
    }

    @Test
    void managesActivitiesAndConvertsTimeGoals() throws Exception {
        var userCookie = login("activity-user@example.com");
        var otherCookie = login("activity-other@example.com");
        var calendarId = createCalendar(userCookie, "Activity calendar", 2);

        mockMvc.perform(get("/api/v1/activities/types").cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[2].label").value("Tempo"))
                .andExpect(jsonPath("$[2].value").value("TIME"));

        mockMvc.perform(get("/api/v1/activities/days").cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].label").value("Segunda-feira"))
                .andExpect(jsonPath("$[1].value").value("MONDAY"));

        var activityId = createActivity(userCookie, calendarId, "Practice", 2, "TIME", "3h 20m");

        mockMvc.perform(get("/api/v1/activities/{id}", activityId).cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goal").value(200))
                .andExpect(jsonPath("$.weekStartsAt").value("2026-01-08"))
                .andExpect(jsonPath("$.weekEndsAt").value("2026-01-14"))
                .andExpect(jsonPath("$.progress").value(0));

        mockMvc.perform(get("/api/v1/activities/{id}", activityId).cookie(otherCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Activity not found"));

        mockMvc.perform(post("/api/v1/activities")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "calendarId",
                                calendarId,
                                "description",
                                "Invalid week",
                                "week",
                                3,
                                "type",
                                "COUNT",
                                "goal",
                                "10"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Activity week must be less than or equal to calendar weeks"));

        mockMvc.perform(put("/api/v1/activities/{id}", activityId)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "calendarId",
                                calendarId,
                                "description",
                                "Read pages",
                                "week",
                                1,
                                "type",
                                "COUNT",
                                "goal",
                                "30"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("COUNT"))
                .andExpect(jsonPath("$.goal").value(30))
                .andExpect(jsonPath("$.weekStartsAt").value("2026-01-01"))
                .andExpect(jsonPath("$.weekEndsAt").value("2026-01-07"))
                .andExpect(jsonPath("$.progress").value(0));
    }

    @Test
    void registersActivityProgress() throws Exception {
        var userCookie = login("progress-user@example.com");
        var calendarId = createCalendar(userCookie, "Progress calendar", 4);
        var daysActivityId = createActivity(userCookie, calendarId, "Train", 1, "DAYS", "3");
        var countActivityId = createActivity(userCookie, calendarId, "Pages", 1, "COUNT", "100");
        var timeActivityId = createActivity(userCookie, calendarId, "Study", 1, "TIME", "5h");

        mockMvc.perform(post("/api/v1/activities/progress/days")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("activityId", daysActivityId, "day", "MONDAY"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress").value(1))
                .andExpect(jsonPath("$.progressDays[0]").value("MONDAY"));

        mockMvc.perform(post("/api/v1/activities/progress/days")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("activityId", daysActivityId, "day", "MONDAY"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Week day already registered for this activity"));

        mockMvc.perform(post("/api/v1/activities/progress/count")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("activityId", countActivityId, "value", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress").value(5));

        mockMvc.perform(post("/api/v1/activities/progress/count")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("activityId", countActivityId, "value", 7))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress").value(12));

        mockMvc.perform(post("/api/v1/activities/progress/time")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("activityId", timeActivityId, "time", "2m 4h"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress").value(242));
    }

    @Test
    void generatesWeeklyPerformanceReportAndStoresMetrics() throws Exception {
        var userCookie = login("report-user@example.com");
        var calendarId = createCalendar(userCookie, "Report calendar", 4);
        var daysActivityId = createActivity(userCookie, calendarId, "Train", 1, "DAYS", "1");
        var countActivityId = createActivity(userCookie, calendarId, "Pages", 1, "COUNT", "10");
        var timeActivityId = createActivity(userCookie, calendarId, "Study", 1, "TIME", "1h");

        mockMvc.perform(post("/api/v1/activities/progress/days")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("activityId", daysActivityId, "day", "MONDAY"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/activities/progress/count")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("activityId", countActivityId, "value", 15))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/activities/progress/time")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("activityId", timeActivityId, "time", "2h"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/calendars/{calendarId}/weeks/{week}/reports", calendarId, 1)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userFeedback", "Foi uma semana produtiva, mas concentrei tudo no fim."))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.calendarId").value(calendarId.toString()))
                .andExpect(jsonPath("$.week").value(1))
                .andExpect(jsonPath("$.weekStartsAt").value("2026-01-01"))
                .andExpect(jsonPath("$.weekEndsAt").value("2026-01-07"))
                .andExpect(jsonPath("$.metrics.expectedTotal").value(71))
                .andExpect(jsonPath("$.metrics.deliveredTotal").value(136))
                .andExpect(jsonPath("$.metrics.deliveryPercentage").value(100.0))
                .andExpect(jsonPath("$.metrics.activities[1].delivered").value(15))
                .andExpect(
                        jsonPath("$.metrics.activities[1].deliveryPercentage").value(100.0))
                .andExpect(jsonPath("$.metrics.activities[2].delivered").value(120))
                .andExpect(
                        jsonPath("$.metrics.activities[2].deliveryPercentage").value(100.0))
                .andExpect(jsonPath("$.markdownReport")
                        .value("# Relatorio Semanal de Desempenho\n\n## Resumo\nRelatorio gerado."));

        mockMvc.perform(post("/api/v1/calendars/{calendarId}/weeks/{week}/reports", calendarId, 1)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userFeedback", "Tentativa duplicada."))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Weekly performance report already exists for this week"));

        mockMvc.perform(get("/api/v1/calendars/{calendarId}/weeks/{week}/reports", calendarId, 1)
                        .cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.week").value(1));

        mockMvc.perform(get("/api/v1/calendars/{calendarId}/reports", calendarId)
                        .cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].week").value(1));
    }

    @Test
    void blocksWeeklyPerformanceReportBeforeWeekEndAndWithoutFeedback() throws Exception {
        var userCookie = login("report-blocked@example.com");
        var calendarId = createCalendar(userCookie, "Future report calendar", 4, "2026-01-02");

        mockMvc.perform(post("/api/v1/calendars/{calendarId}/weeks/{week}/reports", calendarId, 1)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userFeedback", "Ainda nao terminou."))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Weekly performance report can only be generated on or after the week end date"));

        mockMvc.perform(post("/api/v1/calendars/{calendarId}/weeks/{week}/reports", calendarId, 1)
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userFeedback", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User feedback is required"));
    }

    private UUID createGoal(Cookie accessToken, String name, String type) throws Exception {
        var response = mockMvc.perform(post("/api/v1/goals")
                        .cookie(accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", name, "type", type))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse();
        return readId(response.getContentAsString());
    }

    private UUID createCalendar(Cookie accessToken, String description, int weeks, UUID... goalIds) throws Exception {
        return createCalendar(accessToken, description, weeks, "2026-01-01", goalIds);
    }

    private UUID createCalendar(Cookie accessToken, String description, int weeks, String starts, UUID... goalIds)
            throws Exception {
        var response = mockMvc.perform(post("/api/v1/calendars")
                        .cookie(accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "description",
                                description,
                                "weeks",
                                weeks,
                                "starts",
                                starts,
                                "goalIds",
                                java.util.Arrays.asList(goalIds)))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse();
        return readId(response.getContentAsString());
    }

    private UUID createActivity(
            Cookie accessToken, UUID calendarId, String description, int week, String type, String goal)
            throws Exception {
        var response = mockMvc.perform(post("/api/v1/activities")
                        .cookie(accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "calendarId", calendarId,
                                "description", description,
                                "week", week,
                                "type", type,
                                "goal", goal))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse();
        return readId(response.getContentAsString());
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

    @TestConfiguration
    static class TestStorageConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-01-07T12:00:00Z"), ZoneOffset.UTC);
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
