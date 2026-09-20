package com.digitallifetwin.planning.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.digitallifetwin.planning.support.TestJwtFactory;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class DashboardControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void statsUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/stats")).andExpect(status().isUnauthorized());
    }

    @Test
    void statsAreScopedToCurrentUser() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        createTodayTask(owner, "Owner work");

        mockMvc.perform(get("/api/v1/dashboard/stats")
                        .header("Authorization", TestJwtFactory.bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasksTotal").value(1))
                .andExpect(jsonPath("$.aiConfidence").exists());

        mockMvc.perform(get("/api/v1/dashboard/stats")
                        .header("Authorization", TestJwtFactory.bearer(other)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasksTotal").value(0));
    }

    @Test
    void timelineAndUpcomingStayAvailable() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/dashboard/timeline")
                        .header("Authorization", TestJwtFactory.bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/dashboard/upcoming")
                        .header("Authorization", TestJwtFactory.bearer(userId)))
                .andExpect(status().isNoContent());
    }

    private void createTodayTask(UUID userId, String title) throws Exception {
        ZoneId zone = ZoneId.of("Africa/Casablanca");
        ZonedDateTime start = LocalDate.now(zone).atTime(10, 0).atZone(zone);
        String startIso = start.toInstant().toString();
        String deadlineIso = start.plusHours(2).toInstant().toString();
        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", TestJwtFactory.bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "priority": "HIGH",
                                  "plannedDurationMinutes": 60,
                                  "startDateTime": "%s",
                                  "deadline": "%s"
                                }
                                """.formatted(title, startIso, deadlineIso)))
                .andExpect(status().isCreated());
    }
}
