package com.digitallifetwin.wellness.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.digitallifetwin.wellness.support.TestJwtFactory;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
    void dashboardRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/wellness/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardAndWeeklyKeepFrontendContract() throws Exception {
        UUID userId = UUID.randomUUID();
        String auth = TestJwtFactory.bearer(userId);

        mockMvc.perform(get("/api/v1/wellness/dashboard").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sleep.value").exists())
                .andExpect(jsonPath("$.hydration.level").exists())
                .andExpect(jsonPath("$.activity.value").exists())
                .andExpect(jsonPath("$.nutrition.value").exists())
                .andExpect(jsonPath("$.mood.value").exists());

        mockMvc.perform(get("/api/v1/wellness/weekly").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labels").isArray())
                .andExpect(jsonPath("$.sleep").isArray())
                .andExpect(jsonPath("$.activity").isArray())
                .andExpect(jsonPath("$.nutrition").isArray());
    }

    @Test
    void existingWeeklySummaryPathStillWorks() throws Exception {
        mockMvc.perform(get("/api/v1/wellness/summary/weekly")
                        .header("Authorization", TestJwtFactory.bearer(UUID.randomUUID()))
                        .param("startDate", "2026-09-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value("2026-09-20"))
                .andExpect(jsonPath("$.days").isArray());
    }
}
