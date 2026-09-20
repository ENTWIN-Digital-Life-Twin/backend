package com.digitallifetwin.planning.service;

import com.digitallifetwin.planning.client.PlanningAiClient;
import com.digitallifetwin.planning.client.TaskDurationAiRequest;
import com.digitallifetwin.planning.client.TaskDurationAiResponse;
import com.digitallifetwin.planning.config.PlanningProperties;
import com.digitallifetwin.planning.dto.response.DashboardStatsResponse;
import com.digitallifetwin.planning.dto.response.TimelineEventResponse;
import com.digitallifetwin.planning.dto.response.UpcomingEventResponse;
import com.digitallifetwin.planning.entity.CalendarEvent;
import com.digitallifetwin.planning.entity.Task;
import com.digitallifetwin.planning.enums.TaskPriority;
import com.digitallifetwin.planning.enums.TaskStatus;
import com.digitallifetwin.planning.repository.CalendarEventRepository;
import com.digitallifetwin.planning.repository.TaskRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final int BASELINE_AI_CONFIDENCE = 70;

    private final TaskRepository taskRepository;
    private final CalendarEventRepository eventRepository;
    private final PlanningProperties planningProperties;
    private final PlanningAiClient planningAiClient;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats(UUID userId) {
        ZoneId zone = planningProperties.zoneId();
        LocalDate today = LocalDate.now(zone);
        Instant startOfDay = today.atStartOfDay(zone).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant();

        List<Task> todayTasks = taskRepository.findByUserIdAndDueDateBetween(userId, startOfDay, endOfDay);
        int totalTasks = todayTasks.size();
        int completedTasks = (int) todayTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .count();
        int productivityPercent = totalTasks > 0 ? (completedTasks * 100) / totalTasks : 0;

        long focusMinutes = todayTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .mapToLong(this::taskMinutes)
                .sum();
        int breaksTaken = (int) (focusMinutes / 120);

        long goalTasks = todayTasks.stream().filter(this::isGoalTask).count();
        long goalsCompleted = todayTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED && isGoalTask(task))
                .count();
        int goalsMetPercent = goalTasks > 0 ? (int) ((goalsCompleted * 100) / goalTasks) : 0;

        long usableMinutes = Math.max(
                0,
                Duration.between(planningProperties.dayStart(), planningProperties.dayEnd()).toMinutes());
        long workMinutes = focusMinutes + (breaksTaken * 15L);
        long freeMinutes = Math.max(0, usableMinutes - workMinutes);
        long eveningFree = Math.min(freeMinutes, 150);
        long lunchFree = Math.min(freeMinutes, 45);

        return new DashboardStatsResponse(
                productivityPercent,
                completedTasks,
                totalTasks,
                formatDuration(focusMinutes),
                breaksTaken,
                goalsMetPercent,
                aiConfidence(todayTasks, productivityPercent),
                formatDuration(freeMinutes),
                formatDuration(eveningFree),
                formatDuration(lunchFree)
        );
    }

    @Transactional(readOnly = true)
    public List<TimelineEventResponse> getTimeline(UUID userId) {
        ZoneId zone = planningProperties.zoneId();
        LocalDate today = LocalDate.now(zone);
        Instant startOfDay = today.atStartOfDay(zone).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant();

        List<TimelineEventResponse> timeline = new ArrayList<>();
        for (Task task : taskRepository.findByUserIdAndDueDateBetween(userId, startOfDay, endOfDay)) {
            Instant when = task.getStartDateTime() != null ? task.getStartDateTime() : task.getDeadline();
            LocalTime time = when != null
                    ? LocalDateTime.ofInstant(when, zone).toLocalTime()
                    : planningProperties.dayStart();
            timeline.add(new TimelineEventResponse(
                    time.format(TIME),
                    task.getTitle(),
                    task.getDescription() != null ? task.getDescription() : "Task",
                    "work"
            ));
        }
        for (CalendarEvent event : eventRepository.findByUserIdAndStartTimeBetween(userId, startOfDay, endOfDay)) {
            LocalTime time = LocalDateTime.ofInstant(event.getStartDateTime(), zone).toLocalTime();
            String type = event.getEventType() != null
                    ? event.getEventType().name().toLowerCase(Locale.ROOT)
                    : "meeting";
            timeline.add(new TimelineEventResponse(
                    time.format(TIME),
                    event.getTitle(),
                    event.getDescription() != null ? event.getDescription() : "Event",
                    type
            ));
        }
        timeline.sort(Comparator.comparing(TimelineEventResponse::time));
        return timeline;
    }

    @Transactional(readOnly = true)
    public UpcomingEventResponse getUpcomingEvent(UUID userId) {
        ZoneId zone = planningProperties.zoneId();
        Instant now = Instant.now();
        Instant endOfDay = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant();
        List<CalendarEvent> upcoming = eventRepository.findByUserIdAndStartTimeBetween(userId, now, endOfDay);
        if (upcoming.isEmpty()) {
            return null;
        }
        CalendarEvent event = upcoming.getFirst();
        LocalDateTime startTime = LocalDateTime.ofInstant(event.getStartDateTime(), zone);
        LocalDateTime endTime = LocalDateTime.ofInstant(event.getEndDateTime(), zone);
        String location = event.getLocationLabel() != null ? event.getLocationLabel() : "TBD";
        boolean online = location.toLowerCase(Locale.ROOT).contains("online");
        return new UpcomingEventResponse(
                startTime.format(TIME) + " - " + endTime.format(TIME),
                event.getTitle(),
                location,
                online,
                parseParticipants(event.getDescription())
        );
    }

    private int aiConfidence(List<Task> todayTasks, int productivityPercent) {
        Task sample = todayTasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.COMPLETED && task.getStatus() != TaskStatus.CANCELLED)
                .findFirst()
                .orElseGet(() -> todayTasks.isEmpty() ? null : todayTasks.getFirst());
        if (sample == null) {
            return productivityPercent;
        }
        TaskDurationAiRequest request = new TaskDurationAiRequest(
                null,
                sample.getComplexityLevel() != null ? sample.getComplexityLevel().name() : null,
                sample.getEnergyRequired() != null ? sample.getEnergyRequired().name() : null,
                sample.getPlannedDurationMinutes(),
                sample.getActualDurationMinutes() != null && sample.getActualDurationMinutes() > 0
                        ? sample.getActualDurationMinutes()
                        : null
        );
        return planningAiClient.estimateDuration(request)
                .map(this::confidenceFromAi)
                .orElse(productivityPercent);
    }

    private int confidenceFromAi(TaskDurationAiResponse response) {
        if (response.confidence() != null) {
            return (int) Math.round(Math.max(0, Math.min(100, response.confidence() * 100)));
        }
        return BASELINE_AI_CONFIDENCE;
    }

    private boolean isGoalTask(Task task) {
        return task.getPriority() == TaskPriority.HIGH || task.getPriority() == TaskPriority.URGENT;
    }

    private long taskMinutes(Task task) {
        if (task.getActualDurationMinutes() != null && task.getActualDurationMinutes() > 0) {
            return task.getActualDurationMinutes();
        }
        return task.getPlannedDurationMinutes() != null ? task.getPlannedDurationMinutes() : 0;
    }

    private List<String> parseParticipants(String description) {
        List<String> participants = new ArrayList<>();
        if (description == null || !description.contains("participants:")) {
            return participants;
        }
        String[] parts = description.split("participants:", 2);
        if (parts.length < 2) {
            return participants;
        }
        for (String name : parts[1].split(",")) {
            String trimmed = name.trim();
            if (!trimmed.isEmpty()) {
                participants.add(trimmed);
            }
        }
        return participants;
    }

    private String formatDuration(long minutes) {
        long safe = Math.max(0, minutes);
        if (safe < 60) {
            return safe + "m";
        }
        return (safe / 60) + "h " + (safe % 60) + "m";
    }
}
