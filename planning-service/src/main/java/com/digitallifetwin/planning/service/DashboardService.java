package com.digitallifetwin.planning.service;

import com.digitallifetwin.planning.client.PlanningAiClient;
import com.digitallifetwin.planning.client.TaskDurationAiRequest;
import com.digitallifetwin.planning.client.TaskDurationAiResponse;
import com.digitallifetwin.planning.config.PlanningProperties;
import com.digitallifetwin.planning.dto.response.DailyPlanSummaryResponse;
import com.digitallifetwin.planning.dto.response.DashboardStatsResponse;
import com.digitallifetwin.planning.dto.response.TimelineEventResponse;
import com.digitallifetwin.planning.dto.response.UpcomingEventResponse;
import com.digitallifetwin.planning.dto.response.WeeklyProductivityResponse;
import com.digitallifetwin.planning.entity.CalendarEvent;
import com.digitallifetwin.planning.entity.Task;
import com.digitallifetwin.planning.enums.EventType;
import com.digitallifetwin.planning.enums.TaskPriority;
import com.digitallifetwin.planning.enums.TaskStatus;
import com.digitallifetwin.planning.repository.CalendarEventRepository;
import com.digitallifetwin.planning.repository.TaskRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int BASELINE_AI_CONFIDENCE = 70;

    private final TaskRepository taskRepository;
    private final CalendarEventRepository eventRepository;
    private final DailyPlanService dailyPlanService;
    private final PlanningProperties planningProperties;
    private final PlanningAiClient planningAiClient;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats(UUID userId) {
        ZoneId zone = planningProperties.zoneId();
        LocalDate today = LocalDate.now(zone);
        List<Task> todayTasks = tasksForDate(userId, today, zone);
        List<Task> yesterdayTasks = tasksForDate(userId, today.minusDays(1), zone);
        DailyPlanSummaryResponse plan = dailyPlanService.getDailyPlan(userId, today).summary();

        int completed = completedCount(todayTasks);
        int productivity = productivity(todayTasks);
        int productivityChange = productivity - productivity(yesterdayTasks);
        int focusMinutes = todayTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .mapToInt(this::trackedDuration)
                .sum();
        int breaksTaken = focusMinutes / 120;
        List<Task> priorityTasks = todayTasks.stream()
                .filter(task -> task.getPriority() == TaskPriority.HIGH || task.getPriority() == TaskPriority.URGENT)
                .toList();
        int goalsMetPercent = productivity(priorityTasks);
        int freeMinutes = plan.freeMinutes();
        int eveningFree = Math.min(freeMinutes, 150);
        int lunchFree = Math.min(freeMinutes, 45);

        return new DashboardStatsResponse(
                productivity,
                productivityChange,
                completed,
                todayTasks.size(),
                formatDuration(focusMinutes),
                focusMinutes,
                plan.occupiedMinutes(),
                freeMinutes,
                breaksTaken,
                goalsMetPercent,
                goalsMetPercent,
                aiConfidence(todayTasks, productivity),
                formatDuration(freeMinutes),
                formatDuration(eveningFree),
                formatDuration(lunchFree),
                plan.overloaded()
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
            Instant anchor = task.getStartDateTime() != null ? task.getStartDateTime() : task.getDeadline();
            LocalTime time = anchor == null
                    ? planningProperties.dayStart()
                    : LocalDateTime.ofInstant(anchor, zone).toLocalTime();
            timeline.add(new TimelineEventResponse(
                    time.format(TIME_FORMAT),
                    task.getTitle(),
                    task.getDescription() != null ? task.getDescription() : "Task",
                    "work"
            ));
        }

        for (CalendarEvent event : eventRepository.findOverlappingRange(userId, startOfDay, endOfDay)) {
            LocalTime time = LocalDateTime.ofInstant(event.getStartDateTime(), zone).toLocalTime();
            timeline.add(new TimelineEventResponse(
                    time.format(TIME_FORMAT),
                    event.getTitle(),
                    event.getDescription() != null ? event.getDescription() : "Event",
                    timelineType(event.getEventType())
            ));
        }

        timeline.sort((left, right) -> left.time().compareTo(right.time()));
        return timeline;
    }

    @Transactional(readOnly = true)
    public UpcomingEventResponse getUpcomingEvent(UUID userId) {
        ZoneId zone = planningProperties.zoneId();
        Instant now = Instant.now();
        Instant endOfDay = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant();
        CalendarEvent event = eventRepository.findByUserIdAndStartTimeBetween(userId, now, endOfDay).stream()
                .findFirst()
                .orElse(null);

        if (event == null) {
            return null;
        }

        LocalDateTime start = LocalDateTime.ofInstant(event.getStartDateTime(), zone);
        LocalDateTime end = LocalDateTime.ofInstant(event.getEndDateTime(), zone);
        String location = event.getLocationLabel() != null ? event.getLocationLabel() : "TBD";
        boolean online = location.toLowerCase(Locale.ROOT).contains("online");
        String eventType = event.getEventType() != null ? event.getEventType().name() : null;
        return new UpcomingEventResponse(
                event.getId(),
                start.format(TIME_FORMAT) + " - " + end.format(TIME_FORMAT),
                event.getTitle(),
                location,
                online,
                event.getParticipants() == null ? List.of() : List.copyOf(event.getParticipants()),
                eventType
        );
    }

    @Transactional(readOnly = true)
    public WeeklyProductivityResponse getWeeklyProductivity(UUID userId) {
        ZoneId zone = planningProperties.zoneId();
        LocalDate startDate = LocalDate.now(zone).minusDays(6);
        List<String> labels = new ArrayList<>(7);
        List<Integer> productivity = new ArrayList<>(7);
        List<Integer> tasksCompleted = new ArrayList<>(7);
        List<Integer> tasksTotal = new ArrayList<>(7);
        List<Integer> focusMinutes = new ArrayList<>(7);

        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            LocalDate date = startDate.plusDays(dayOffset);
            List<Task> tasks = tasksForDate(userId, date, zone);
            labels.add(date.getDayOfWeek().name().substring(0, 3));
            productivity.add(productivity(tasks));
            tasksCompleted.add(completedCount(tasks));
            tasksTotal.add(tasks.size());
            focusMinutes.add(tasks.stream()
                    .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                    .mapToInt(this::trackedDuration)
                    .sum());
        }

        return new WeeklyProductivityResponse(labels, productivity, tasksCompleted, tasksTotal, focusMinutes);
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

    private List<Task> tasksForDate(UUID userId, LocalDate date, ZoneId zone) {
        return taskRepository.findByUserIdAndDueDateBetween(
                userId,
                date.atStartOfDay(zone).toInstant(),
                date.plusDays(1).atStartOfDay(zone).toInstant()
        );
    }

    private int completedCount(List<Task> tasks) {
        return (int) tasks.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
    }

    private int productivity(List<Task> tasks) {
        return tasks.isEmpty() ? 0 : completedCount(tasks) * 100 / tasks.size();
    }

    private int trackedDuration(Task task) {
        if (task.getActualDurationMinutes() != null) {
            return task.getActualDurationMinutes();
        }
        return task.getPlannedDurationMinutes() != null ? task.getPlannedDurationMinutes() : 0;
    }

    private String formatDuration(long minutes) {
        long safe = Math.max(0, minutes);
        if (safe < 60) {
            return safe + "m";
        }
        return (safe / 60) + "h " + (safe % 60) + "m";
    }

    private String timelineType(EventType eventType) {
        if (eventType == null) {
            return "other";
        }
        return switch (eventType) {
            case WORK -> "work";
            case PERSONAL -> "personal";
            case APPOINTMENT -> "meeting";
            case STUDY -> "study";
            case HEALTH -> "health";
            case SPORT -> "sport";
            case OTHER -> "other";
        };
    }
}
