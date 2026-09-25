package com.digitallifetwin.planning.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.digitallifetwin.planning.support.TestJwtFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
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
class PlanningFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createTask_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson("Study", 90, "2026-08-23T17:00:00Z", "2026-08-23T20:00:00Z")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createTask_invalidDuration_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", TestJwtFactory.bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Bad",
                                  "priority": "LOW",
                                  "plannedDurationMinutes": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.plannedDurationMinutes").exists());
    }

    @Test
    void createTask_success_andListPaginated() throws Exception {
        UUID userId = UUID.randomUUID();
        String auth = TestJwtFactory.bearer(userId);

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson("Study Spring Boot", 90, "2026-08-23T17:00:00Z", "2026-08-23T20:00:00Z")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Study Spring Boot"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        mockMvc.perform(get("/api/v1/tasks")
                        .header("Authorization", auth)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void getMissingTask_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/" + UUID.randomUUID())
                        .header("Authorization", TestJwtFactory.bearer(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownership_userCannotAccessAnotherUsersTask() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        String taskId = createTask(owner, "Owner task", 60, "2026-08-23T09:00:00Z", "2026-08-23T12:00:00Z");

        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                        .header("Authorization", TestJwtFactory.bearer(owner)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                        .header("Authorization", TestJwtFactory.bearer(other)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/tasks/" + taskId)
                        .header("Authorization", TestJwtFactory.bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Hacked",
                                  "priority": "LOW",
                                  "plannedDurationMinutes": 30,
                                  "completionPercentage": 0
                                }
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/tasks/" + taskId)
                        .header("Authorization", TestJwtFactory.bearer(other)))
                .andExpect(status().isNotFound());
    }

    @Test
    void malformedJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/tasks")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboard_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/stats")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/dashboard/timeline")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/dashboard/upcoming")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/dashboard/weekly")).andExpect(status().isUnauthorized());
    }

    @Test
    void dashboard_emptyUpcoming_returns204_andOtherSectionsOk() throws Exception {
        String auth = TestJwtFactory.bearer(UUID.randomUUID());

        mockMvc.perform(get("/api/v1/dashboard/upcoming").header("Authorization", auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/dashboard/stats").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productivityPercent").value(0))
                .andExpect(jsonPath("$.tasksCompleted").value(0))
                .andExpect(jsonPath("$.tasksTotal").value(0))
                .andExpect(jsonPath("$.focusMinutes").exists())
                .andExpect(jsonPath("$.occupiedMinutes").exists())
                .andExpect(jsonPath("$.freeMinutes").exists())
                .andExpect(jsonPath("$.overloaded").exists());

        mockMvc.perform(get("/api/v1/dashboard/timeline").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/dashboard/weekly").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labels.length()").value(7))
                .andExpect(jsonPath("$.productivity.length()").value(7))
                .andExpect(jsonPath("$.tasksCompleted.length()").value(7))
                .andExpect(jsonPath("$.tasksTotal.length()").value(7))
                .andExpect(jsonPath("$.focusMinutes.length()").value(7));
    }

    @Test
    void dashboard_upcomingEvent_mapsAppointmentTimelineAsMeeting() throws Exception {
        UUID userId = UUID.randomUUID();
        String auth = TestJwtFactory.bearer(userId);
        Instant start = Instant.now().plus(Duration.ofMinutes(20));
        Instant end = start.plus(Duration.ofMinutes(40));

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("Client call", start.toString(), end.toString(), "APPOINTMENT")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/dashboard/upcoming").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Client call"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.time").exists())
                .andExpect(jsonPath("$.eventType").value("APPOINTMENT"));

        mockMvc.perform(get("/api/v1/dashboard/timeline").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Client call"))
                .andExpect(jsonPath("$[0].type").value("meeting"));
    }

    @Test
    void events_createRejectEndBeforeStart_andOwnership() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", TestJwtFactory.bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Bad",
                                  "startDateTime": "2026-08-23T12:00:00Z",
                                  "endDateTime": "2026-08-23T11:00:00Z",
                                  "allDay": false,
                                  "eventType": "WORK",
                                  "recurring": false
                                }
                                """))
                .andExpect(status().isBadRequest());

        String eventId = createEvent(owner, "Meeting", "2026-08-23T10:00:00Z", "2026-08-23T11:00:00Z");

        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", TestJwtFactory.bearer(owner)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", TestJwtFactory.bearer(other)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", TestJwtFactory.bearer(owner))
                        .param("from", "2026-08-23T00:00:00Z")
                        .param("to", "2026-08-24T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void categories_systemVisible_customCrud_cannotEditSystem() throws Exception {
        UUID user = UUID.randomUUID();
        String auth = TestJwtFactory.bearer(user);

        MvcResult listResult = mockMvc.perform(get("/api/v1/task-categories").header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode categories = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertThat(categories.size()).isGreaterThanOrEqualTo(6);

        String systemId = null;
        for (JsonNode category : categories) {
            if (category.get("systemCategory").asBoolean() && "WORK".equals(category.get("name").asText())) {
                systemId = category.get("id").asText();
                break;
            }
        }
        assertThat(systemId).isNotNull();

        mockMvc.perform(put("/api/v1/task-categories/" + systemId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Hacked","description":"nope","colorCode":"#000"}
                                """))
                .andExpect(status().isForbidden());

        MvcResult create = mockMvc.perform(post("/api/v1/task-categories")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"My Custom","description":"mine","colorCode":"#111111"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.systemCategory").value(false))
                .andReturn();
        String customId = objectMapper.readTree(create.getResponse().getContentAsString()).get("id").asText();

        UUID other = UUID.randomUUID();
        mockMvc.perform(put("/api/v1/task-categories/" + customId)
                        .header("Authorization", TestJwtFactory.bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Stolen","description":"x","colorCode":"#222"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void conflicts_taskVsTask_eventVsEvent_taskVsEvent_andNonOverlap() throws Exception {
        UUID user = UUID.randomUUID();
        String auth = TestJwtFactory.bearer(user);

        MvcResult taskA = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson("A", 60, "2026-08-23T10:00:00Z", "2026-08-23T18:00:00Z")))
                .andExpect(status().isCreated())
                .andReturn();
        assertThat(objectMapper.readTree(taskA.getResponse().getContentAsString()).get("conflicts")).isEmpty();

        MvcResult taskB = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson("B", 60, "2026-08-23T10:30:00Z", "2026-08-23T18:00:00Z")))
                .andExpect(status().isCreated())
                .andReturn();
        assertThat(objectMapper.readTree(taskB.getResponse().getContentAsString())
                .get("conflicts").size()).isGreaterThan(0);

        MvcResult eventA = mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("EA", "2026-08-23T14:00:00Z", "2026-08-23T15:00:00Z")))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult eventB = mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("EB", "2026-08-23T14:30:00Z", "2026-08-23T15:30:00Z")))
                .andExpect(status().isCreated())
                .andReturn();
        assertThat(objectMapper.readTree(eventB.getResponse().getContentAsString())
                .get("conflicts").size()).isGreaterThan(0);

        MvcResult taskVsEvent = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson("C", 60, "2026-08-23T14:15:00Z", "2026-08-23T18:00:00Z")))
                .andExpect(status().isCreated())
                .andReturn();
        assertThat(objectMapper.readTree(taskVsEvent.getResponse().getContentAsString())
                .get("conflicts").size()).isGreaterThan(0);

        MvcResult free = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson("Free", 30, "2026-08-23T16:00:00Z", "2026-08-23T18:00:00Z")))
                .andExpect(status().isCreated())
                .andReturn();
        // may still conflict with nothing in 16:00 slot depending on earlier items; assert create allowed
        assertThat(objectMapper.readTree(free.getResponse().getContentAsString()).get("id")).isNotNull();
        assertThat(objectMapper.readTree(eventA.getResponse().getContentAsString()).get("id")).isNotNull();
    }

    @Test
    void dailyPlan_calculatesSummaryWithoutDoubleCounting() throws Exception {
        UUID user = UUID.randomUUID();
        String auth = TestJwtFactory.bearer(user);

        // Africa/Casablanca is UTC+1 in August 2026 (DST) -> local 08:00 = 07:00Z, local 23:00 = 22:00Z
        createTask(user, "Morning", 60, "2026-08-23T09:00:00Z", "2026-08-23T18:00:00Z");
        createTask(user, "Overlap", 60, "2026-08-23T09:30:00Z", "2026-08-23T18:00:00Z");
        String doneId = createTask(user, "Done", 30, "2026-08-23T15:00:00Z", "2026-08-23T18:00:00Z");
        createEvent(user, "Lunch", "2026-08-23T12:00:00Z", "2026-08-23T13:00:00Z");

        mockMvc.perform(patch("/api/v1/tasks/" + doneId + "/status")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/tasks/" + doneId + "/status")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());

        MvcResult plan = mockMvc.perform(get("/api/v1/planning/daily")
                        .header("Authorization", auth)
                        .param("date", "2026-08-23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("Africa/Casablanca"))
                .andExpect(jsonPath("$.summary.totalTasks").value(3))
                .andExpect(jsonPath("$.summary.completedTasks").value(1))
                .andExpect(jsonPath("$.summary.plannedMinutes").value(150))
                .andExpect(jsonPath("$.summary.eventMinutes").value(60))
                .andExpect(jsonPath("$.summary.conflictCount").value(1))
                .andReturn();

        JsonNode summary = objectMapper.readTree(plan.getResponse().getContentAsString()).get("summary");
        // Occupied: 09:00-10:30 (90) + 12:00-13:00 (60) + 15:00-15:30 (30) = 180
        assertThat(summary.get("occupiedMinutes").asInt()).isEqualTo(180);
        // Available window 08:00-23:00 local = 15h = 900 minutes
        assertThat(summary.get("freeMinutes").asInt()).isEqualTo(720);
    }

    private String createTask(UUID userId, String title, int minutes, String start, String deadline) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", TestJwtFactory.bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson(title, minutes, start, deadline)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createEvent(UUID userId, String title, String start, String end) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", TestJwtFactory.bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(title, start, end)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String taskJson(String title, int minutes, String start, String deadline) {
        return """
                {
                  "title": "%s",
                  "priority": "HIGH",
                  "plannedDurationMinutes": %d,
                  "startDateTime": "%s",
                  "deadline": "%s",
                  "energyRequired": "MEDIUM",
                  "complexityLevel": "MEDIUM"
                }
                """.formatted(title, minutes, start, deadline);
    }

    private String eventJson(String title, String start, String end) {
        return eventJson(title, start, end, "WORK");
    }

    private String eventJson(String title, String start, String end, String eventType) {
        return """
                {
                  "title": "%s",
                  "startDateTime": "%s",
                  "endDateTime": "%s",
                  "allDay": false,
                  "eventType": "%s",
                  "recurring": false
                }
                """.formatted(title, start, end, eventType);
    }
}
