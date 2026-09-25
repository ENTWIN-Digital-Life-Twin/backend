package com.digitallifetwin.wellness.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.digitallifetwin.wellness.support.TestJwtFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class WellnessFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void dashboard_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/wellness/dashboard")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/wellness/weekly")).andExpect(status().isUnauthorized());
    }

    @Test
    void dashboard_returnsTodayMetricsAndWeeklySeries() throws Exception {
        String auth = TestJwtFactory.bearer(UUID.randomUUID());

        mockMvc.perform(get("/api/v1/wellness/dashboard").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sleep.value").exists())
                .andExpect(jsonPath("$.sleep.level").exists())
                .andExpect(jsonPath("$.hydration.value").exists())
                .andExpect(jsonPath("$.hydration.level").exists())
                .andExpect(jsonPath("$.activity.value").exists())
                .andExpect(jsonPath("$.nutrition.value").exists())
                .andExpect(jsonPath("$.mood.value").exists());

        mockMvc.perform(get("/api/v1/wellness/weekly").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labels.length()").value(7))
                .andExpect(jsonPath("$.sleep.length()").value(7))
                .andExpect(jsonPath("$.activity.length()").value(7))
                .andExpect(jsonPath("$.nutrition.length()").value(7));
    }

    @Test
    void createSleep_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/wellness/sleep")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sleepJson("2026-08-21T22:00:00Z", "2026-08-22T06:00:00Z", 8)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/wellness/sleep")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createSleep_computesDuration() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/wellness/sleep")
                        .header("Authorization", TestJwtFactory.bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sleepJson("2026-08-21T22:00:00Z", "2026-08-22T06:00:00Z", 8)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.durationMinutes").value(480))
                .andExpect(jsonPath("$.qualityScore").value(8));
    }

    @Test
    void ownership_otherUserGets404() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        String sleepId = createSleep(owner, "2026-08-21T22:00:00Z", "2026-08-22T06:00:00Z", 7);

        mockMvc.perform(get("/api/v1/wellness/sleep/" + sleepId)
                        .header("Authorization", TestJwtFactory.bearer(owner)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/wellness/sleep/" + sleepId)
                        .header("Authorization", TestJwtFactory.bearer(other)))
                .andExpect(status().isNotFound());
    }

    @Test
    void softDelete_excludesFromListAndSummary() throws Exception {
        UUID userId = UUID.randomUUID();
        String auth = TestJwtFactory.bearer(userId);
        String sleepId = createSleep(userId, "2026-08-21T23:00:00Z", "2026-08-22T07:00:00Z", 6);

        mockMvc.perform(delete("/api/v1/wellness/sleep/" + sleepId).header("Authorization", auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/wellness/sleep").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/wellness/summary/daily")
                        .header("Authorization", auth)
                        .param("date", "2026-08-22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sleep.totalMinutes").value(0));
    }

    @Test
    void waterTotal_andMoodValidation_andGoalTransition() throws Exception {
        UUID userId = UUID.randomUUID();
        String auth = TestJwtFactory.bearer(userId);

        mockMvc.perform(post("/api/v1/wellness/water")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantityMl": 250,
                                  "consumedAt": "2026-08-22T10:00:00Z",
                                  "beverageType": "TEA"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/wellness/water")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantityMl": 500,
                                  "consumedAt": "2026-08-22T14:00:00Z",
                                  "beverageType": "WATER"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/wellness/summary/daily")
                        .header("Authorization", auth)
                        .param("date", "2026-08-22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hydration.totalMl").value(700));

        mockMvc.perform(post("/api/v1/wellness/mood")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recordedAt": "2026-08-22T12:00:00Z",
                                  "moodLevel": 11,
                                  "stressLevel": 5,
                                  "fatigueLevel": 5
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.moodLevel").exists());

        mockMvc.perform(post("/api/v1/wellness/mood")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recordedAt": "2026-08-22T12:00:00Z",
                                  "moodLevel": 8,
                                  "stressLevel": 3,
                                  "fatigueLevel": 4
                                }
                                """))
                .andExpect(status().isCreated());

        MvcResult goalResult = mockMvc.perform(post("/api/v1/wellness/goals")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goalType": "DAILY_WATER",
                                  "targetValue": 2000,
                                  "currentValue": 0,
                                  "unit": "ml",
                                  "startDate": "2026-08-22"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        String goalId = objectMapper.readTree(goalResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch("/api/v1/wellness/goals/" + goalId + "/status")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").exists());
    }

    @Test
    void dailyAndWeeklySummary_numbers() throws Exception {
        UUID userId = UUID.randomUUID();
        String auth = TestJwtFactory.bearer(userId);

        createSleep(userId, "2026-08-21T22:00:00Z", "2026-08-22T06:00:00Z", 8);

        mockMvc.perform(post("/api/v1/wellness/meals")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mealType": "BREAKFAST",
                                  "description": "Oatmeal",
                                  "mealTime": "2026-08-22T08:00:00Z",
                                  "totalCalories": 400,
                                  "proteinGrams": 15,
                                  "carbohydrateGrams": 60,
                                  "fatGrams": 10
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/wellness/workouts")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activityType": "WALKING",
                                  "startedAt": "2026-08-22T18:00:00Z",
                                  "durationMinutes": 30,
                                  "intensity": "MODERATE",
                                  "caloriesBurned": 150
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/wellness/health-records")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recordedAt": "2026-08-22T21:00:00Z",
                                  "stepCount": 9000
                                }
                                """))
                .andExpect(status().isCreated());

        MvcResult daily = mockMvc.perform(get("/api/v1/wellness/summary/daily")
                        .header("Authorization", auth)
                        .param("date", "2026-08-22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sleep.totalMinutes").value(480))
                .andExpect(jsonPath("$.nutrition.totalCalories").value(400.0))
                .andExpect(jsonPath("$.activity.activeMinutes").value(30))
                .andExpect(jsonPath("$.activity.steps").value(9000))
                .andReturn();

        JsonNode dailyJson = objectMapper.readTree(daily.getResponse().getContentAsString());
        assertThat(dailyJson.get("timezone").asText()).isEqualTo("Africa/Casablanca");

        mockMvc.perform(get("/api/v1/wellness/summary/weekly")
                        .header("Authorization", auth)
                        .param("startDate", "2026-08-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days.length()").value(7))
                .andExpect(jsonPath("$.averageSleepMinutes").value(480.0))
                .andExpect(jsonPath("$.totalWorkoutMinutes").value(30))
                .andExpect(jsonPath("$.workoutCount").value(1))
                .andExpect(jsonPath("$.totalCaloriesConsumed").value(400.0))
                .andExpect(jsonPath("$.averageDailySteps").value(9000.0 / 7.0));
    }

    private String createSleep(UUID userId, String start, String wake, int quality) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/wellness/sleep")
                        .header("Authorization", TestJwtFactory.bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sleepJson(start, wake, quality)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private static String sleepJson(String start, String wake, int quality) {
        return """
                {
                  "sleepStart": "%s",
                  "wakeTime": "%s",
                  "qualityScore": %d
                }
                """.formatted(start, wake, quality);
    }
}
