package com.digitallifetwin.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.digitallifetwin.planning.client.PlanningAiClient;
import com.digitallifetwin.planning.config.PlanningProperties;
import com.digitallifetwin.planning.dto.response.DailyPlanResponse;
import com.digitallifetwin.planning.dto.response.DailyPlanSummaryResponse;
import com.digitallifetwin.planning.dto.response.DashboardStatsResponse;
import com.digitallifetwin.planning.entity.CalendarEvent;
import com.digitallifetwin.planning.entity.Task;
import com.digitallifetwin.planning.enums.EventType;
import com.digitallifetwin.planning.enums.TaskPriority;
import com.digitallifetwin.planning.enums.TaskStatus;
import com.digitallifetwin.planning.repository.CalendarEventRepository;
import com.digitallifetwin.planning.repository.TaskRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private CalendarEventRepository eventRepository;
    @Mock
    private DailyPlanService dailyPlanService;
    @Mock
    private PlanningAiClient planningAiClient;

    private DashboardService dashboardService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        PlanningProperties properties = new PlanningProperties(
                "Africa/Casablanca", LocalTime.of(8, 0), LocalTime.of(23, 0), 0.9);
        dashboardService = new DashboardService(
                taskRepository, eventRepository, dailyPlanService, properties, planningAiClient);
    }

    @Test
    void statsUseTrackedTasksAndDailyPlanSummary() {
        Task open = task(TaskStatus.SCHEDULED, TaskPriority.HIGH, 60, null);
        Task done = task(TaskStatus.COMPLETED, TaskPriority.HIGH, 45, 40);
        when(taskRepository.findByUserIdAndDueDateBetween(eq(userId), any(), any()))
                .thenReturn(List.of(open, done), List.of(task(TaskStatus.COMPLETED, TaskPriority.MEDIUM, 30, 30)));
        when(dailyPlanService.getDailyPlan(eq(userId), any(LocalDate.class)))
                .thenReturn(new DailyPlanResponse(
                        LocalDate.now(),
                        "Africa/Casablanca",
                        List.of(),
                        List.of(),
                        new DailyPlanSummaryResponse(2, 1, 105, 30, 120, 780, 0, false)
                ));
        when(planningAiClient.estimateDuration(any())).thenReturn(Optional.empty());

        DashboardStatsResponse stats = dashboardService.getStats(userId);

        assertThat(stats.tasksTotal()).isEqualTo(2);
        assertThat(stats.tasksCompleted()).isEqualTo(1);
        assertThat(stats.productivityPercent()).isEqualTo(50);
        assertThat(stats.productivityChangePercent()).isEqualTo(-50);
        assertThat(stats.priorityGoalsMetPercent()).isEqualTo(50);
        assertThat(stats.goalsMetPercent()).isEqualTo(50);
        assertThat(stats.focusMinutes()).isEqualTo(40);
        assertThat(stats.focusTime()).isEqualTo("40m");
        assertThat(stats.occupiedMinutes()).isEqualTo(120);
        assertThat(stats.freeMinutes()).isEqualTo(780);
        assertThat(stats.freeTimeTotal()).isEqualTo("13h 0m");
        assertThat(stats.aiConfidence()).isEqualTo(50);
        assertThat(stats.overloaded()).isFalse();
    }

    @Test
    void upcomingIsNullWhenUserHasNoEvents() {
        when(eventRepository.findByUserIdAndStartTimeBetween(eq(userId), any(), any())).thenReturn(List.of());
        assertThat(dashboardService.getUpcomingEvent(userId)).isNull();
    }

    @Test
    void upcomingUsesStoredParticipants() {
        CalendarEvent event = new CalendarEvent();
        event.setId(UUID.randomUUID());
        event.setTitle("Standup");
        event.setStartDateTime(Instant.now().plusSeconds(3600));
        event.setEndDateTime(Instant.now().plusSeconds(5400));
        event.setLocationLabel("Room A");
        event.setEventType(EventType.WORK);
        event.setParticipants(List.of("Ada", "Karim"));
        when(eventRepository.findByUserIdAndStartTimeBetween(eq(userId), any(), any()))
                .thenReturn(List.of(event));

        assertThat(dashboardService.getUpcomingEvent(userId).participants())
                .containsExactly("Ada", "Karim");
    }

    private Task task(TaskStatus status, TaskPriority priority, int planned, Integer actual) {
        Task task = new Task();
        task.setUserId(userId);
        task.setTitle("Task");
        task.setStatus(status);
        task.setPriority(priority);
        task.setPlannedDurationMinutes(planned);
        task.setActualDurationMinutes(actual);
        task.setDeadline(Instant.now());
        return task;
    }
}
