package com.digitallifetwin.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.digitallifetwin.notification.entity.Notification;
import com.digitallifetwin.notification.entity.Reminder;
import com.digitallifetwin.notification.enums.NotificationStatus;
import com.digitallifetwin.notification.enums.RecurrenceType;
import com.digitallifetwin.notification.repository.NotificationRepository;
import com.digitallifetwin.notification.repository.ReminderRepository;
import com.digitallifetwin.notification.service.ReminderExecutionService;
import com.digitallifetwin.notification.support.TestJwtFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class NotificationFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReminderExecutionService reminderExecutionService;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void cleanDatabase() {
        notificationRepository.deleteAll();
        reminderRepository.deleteAll();
    }

    @Test
    void createReminder_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oneTimeReminderJson("Drink water", "2026-08-24T10:00:00Z")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/reminders")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReminder_validation() throws Exception {
        UUID userId = UUID.randomUUID();
        String auth = TestJwtFactory.bearer(userId);

        mockMvc.perform(post("/api/v1/reminders")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oneTimeReminderJson("Drink water", "2026-08-24T10:00:00Z")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Drink water"))
                .andExpect(jsonPath("$.enabled").value(true));

        mockMvc.perform(post("/api/v1/reminders")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "reminderType": "WATER",
                                  "triggerDateTime": "2026-08-24T10:00:00Z",
                                  "recurring": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());

        mockMvc.perform(post("/api/v1/reminders")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "No trigger",
                                  "reminderType": "WATER",
                                  "recurring": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.triggerDateTime").exists());

        mockMvc.perform(post("/api/v1/reminders")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Negative advance",
                                  "reminderType": "WATER",
                                  "triggerDateTime": "2026-08-24T10:00:00Z",
                                  "recurring": false,
                                  "advanceMinutes": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.advanceMinutes").exists());

        mockMvc.perform(post("/api/v1/reminders")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Daily water",
                                  "reminderType": "WATER",
                                  "triggerDateTime": "2026-08-24T10:00:00Z",
                                  "recurring": true
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ownership_otherUserGets404() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        String reminderId = createReminder(owner, oneTimeReminderJson("Mine", "2026-08-24T10:00:00Z"));

        mockMvc.perform(get("/api/v1/reminders/" + reminderId)
                        .header("Authorization", TestJwtFactory.bearer(owner)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reminders/" + reminderId)
                        .header("Authorization", TestJwtFactory.bearer(other)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/reminders/" + reminderId)
                        .header("Authorization", TestJwtFactory.bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oneTimeReminderJson("Hijack", "2026-08-24T11:00:00Z")))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/reminders/" + reminderId)
                        .header("Authorization", TestJwtFactory.bearer(other)))
                .andExpect(status().isNotFound());
    }

    @Test
    void pagination_andInvalidSort_return400() throws Exception {
        UUID userId = UUID.randomUUID();
        String auth = TestJwtFactory.bearer(userId);
        createReminder(userId, oneTimeReminderJson("One", "2026-08-24T10:00:00Z"));
        createReminder(userId, oneTimeReminderJson("Two", "2026-08-24T11:00:00Z"));

        mockMvc.perform(get("/api/v1/reminders")
                        .header("Authorization", auth)
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/v1/reminders")
                        .header("Authorization", auth)
                        .param("sort", "notAField,desc"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/reminders")
                        .header("Authorization", auth)
                        .param("reminderType", "NOPE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void scheduler_dueReminder_persistsNotification_andUpdatesReminder() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant past = Instant.now().minus(10, ChronoUnit.MINUTES);
        String reminderId = createReminder(userId, oneTimeReminderJson("Due water", past.toString()));

        Instant now = Instant.now();
        int processed = reminderExecutionService.processDueReminders(now);
        assertThat(processed).isGreaterThanOrEqualTo(1);

        Reminder reminder = reminderRepository.findById(UUID.fromString(reminderId)).orElseThrow();
        assertThat(reminder.isEnabled()).isFalse();
        assertThat(reminder.getLastTriggeredAt()).isNotNull();

        List<Notification> notifications = notificationRepository.findAll().stream()
                .filter(n -> reminder.getId().equals(n.getReminderId()))
                .toList();
        assertThat(notifications).hasSize(1);
        assertThat(notifications.getFirst().getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notifications.getFirst().getChannel().name()).isEqualTo("IN_APP");
        assertThat(notifications.getFirst().getSentAt()).isNotNull();

        reminderExecutionService.processDueReminders(Instant.now());
        long countAfter = notificationRepository.findAll().stream()
                .filter(n -> reminder.getId().equals(n.getReminderId()))
                .count();
        assertThat(countAfter).isEqualTo(1);
        Reminder afterSecond = reminderRepository.findById(UUID.fromString(reminderId)).orElseThrow();
        assertThat(afterSecond.isEnabled()).isFalse();
    }

    @Test
    void scheduler_skipsFutureDisabledAndDeleted() throws Exception {
        UUID userId = UUID.randomUUID();
        String auth = TestJwtFactory.bearer(userId);

        String futureId = createReminder(userId, oneTimeReminderJson(
                "Future", Instant.now().plus(2, ChronoUnit.HOURS).toString()));
        String disabledId = createReminder(userId, """
                {
                  "title": "Disabled",
                  "reminderType": "WATER",
                  "triggerDateTime": "%s",
                  "recurring": false,
                  "enabled": false,
                  "advanceMinutes": 0
                }
                """.formatted(Instant.now().minus(5, ChronoUnit.MINUTES)));
        String deletedId = createReminder(userId, oneTimeReminderJson(
                "Deleted", Instant.now().minus(5, ChronoUnit.MINUTES).toString()));
        mockMvc.perform(delete("/api/v1/reminders/" + deletedId).header("Authorization", auth))
                .andExpect(status().isNoContent());

        reminderExecutionService.processDueReminders(Instant.now());

        assertThat(notificationRepository.findAll().stream()
                .filter(n -> UUID.fromString(futureId).equals(n.getReminderId())
                        || UUID.fromString(disabledId).equals(n.getReminderId())
                        || UUID.fromString(deletedId).equals(n.getReminderId()))
                .count()).isZero();
        assertThat(reminderRepository.findById(UUID.fromString(futureId)).orElseThrow().isEnabled()).isTrue();
    }

    @Test
    void scheduler_weeklyRecurrence_advancesSevenDays() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant trigger = Instant.now().minus(1, ChronoUnit.MINUTES);
        MvcResult result = mockMvc.perform(post("/api/v1/reminders")
                        .header("Authorization", TestJwtFactory.bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Weekly workout",
                                  "reminderType": "WORKOUT",
                                  "triggerDateTime": "%s",
                                  "recurring": true,
                                  "recurrenceType": "WEEKLY",
                                  "enabled": true,
                                  "advanceMinutes": 0
                                }
                                """.formatted(trigger)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID reminderId = UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());

        reminderExecutionService.processDueReminders(Instant.now());

        Reminder reminder = reminderRepository.findById(reminderId).orElseThrow();
        assertThat(reminder.isEnabled()).isTrue();
        assertThat(reminder.getRecurrenceType()).isEqualTo(RecurrenceType.WEEKLY);
        assertThat(reminder.getNextTriggerAt()).isEqualTo(reminder.getTriggerDateTime().plus(7, ChronoUnit.DAYS));
    }

    @Test
    void notifications_readUnreadCountSoftDeleteAndOwnership() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        String ownerAuth = TestJwtFactory.bearer(owner);
        String otherAuth = TestJwtFactory.bearer(other);

        String reminderId = createReminder(owner, oneTimeReminderJson(
                "Due", Instant.now().minus(2, ChronoUnit.MINUTES).toString()));
        reminderExecutionService.processDueReminders(Instant.now());

        MvcResult list = mockMvc.perform(get("/api/v1/notifications").header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andReturn();
        String notificationId = objectMapper.readTree(list.getResponse().getContentAsString())
                .get("content").get(0).get("id").asText();

        mockMvc.perform(get("/api/v1/notifications/" + notificationId).header("Authorization", otherAuth))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read")
                        .header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"))
                .andExpect(jsonPath("$.readAt").exists());

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read")
                        .header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"));

        mockMvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        createReminder(owner, oneTimeReminderJson(
                "Second", Instant.now().minus(1, ChronoUnit.MINUTES).toString()));
        reminderExecutionService.processDueReminders(Instant.now());

        mockMvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        mockMvc.perform(patch("/api/v1/notifications/read-all").header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        mockMvc.perform(delete("/api/v1/notifications/" + notificationId).header("Authorization", ownerAuth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/notifications").header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + notificationId + "')]").doesNotExist());

        mockMvc.perform(get("/api/v1/notifications/" + notificationId).header("Authorization", ownerAuth))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/reminders/" + reminderId).header("Authorization", ownerAuth))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/reminders/" + reminderId).header("Authorization", ownerAuth))
                .andExpect(status().isNotFound());

        JsonNode reminderList = objectMapper.readTree(mockMvc.perform(get("/api/v1/reminders")
                        .header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        assertThat(reminderList.get("content").toString()).doesNotContain(reminderId);
    }

    @Test
    void patchEnabled() throws Exception {
        UUID userId = UUID.randomUUID();
        String auth = TestJwtFactory.bearer(userId);
        String reminderId = createReminder(userId, oneTimeReminderJson("Toggle", "2026-08-24T10:00:00Z"));

        mockMvc.perform(patch("/api/v1/reminders/" + reminderId + "/enabled")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    private String createReminder(UUID userId, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/reminders")
                        .header("Authorization", TestJwtFactory.bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private static String oneTimeReminderJson(String title, String triggerDateTime) {
        return """
                {
                  "title": "%s",
                  "message": "Time to drink some water",
                  "reminderType": "WATER",
                  "triggerDateTime": "%s",
                  "recurring": false,
                  "recurrenceType": "NONE",
                  "enabled": true,
                  "sourceType": "CUSTOM",
                  "sourceResourceId": null,
                  "advanceMinutes": 0
                }
                """.formatted(title, triggerDateTime);
    }
}
