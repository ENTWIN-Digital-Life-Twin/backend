package com.digitallifetwin.planning.service;

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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final TaskRepository taskRepository;
    private final CalendarEventRepository eventRepository;
    private final DailyPlanService dailyPlanService;
    private final PlanningProperties planningProperties;

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
        List<Task> priorityTasks = todayTasks.stream()
                .filter(task -> task.getPriority() == TaskPriority.HIGH || task.getPriority() == TaskPriority.URGENT)
                .toList();

        return new DashboardStatsResponse(
                productivity,
                productivityChange,
                completed,
                todayTasks.size(),
                focusMinutes,
                plan.occupiedMinutes(),
                plan.freeMinutes(),
                productivity(priorityTasks),
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
                    task.getDescription() != null ? task.getDescription() : "",
                    "work"
            ));
        }

        for (CalendarEvent event : eventRepository.findOverlappingRange(userId, startOfDay, endOfDay)) {
            LocalTime time = LocalDateTime.ofInstant(event.getStartDateTime(), zone).toLocalTime();
            timeline.add(new TimelineEventResponse(
                    time.format(TIME_FORMAT),
                    event.getTitle(),
                    event.getDescription() != null ? event.getDescription() : "",
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
        return new UpcomingEventResponse(
                event.getId(),
                start.format(TIME_FORMAT) + " - " + end.format(TIME_FORMAT),
                event.getTitle(),
                event.getLocationLabel(),
                event.getEventType().name()
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
