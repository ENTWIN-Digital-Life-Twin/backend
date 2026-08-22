package com.digitallifetwin.planning.service;

import com.digitallifetwin.planning.config.PlanningProperties;
import com.digitallifetwin.planning.dto.response.DailyPlanResponse;
import com.digitallifetwin.planning.dto.response.DailyPlanSummaryResponse;
import com.digitallifetwin.planning.dto.response.EventResponse;
import com.digitallifetwin.planning.dto.response.ScheduleConflictResponse;
import com.digitallifetwin.planning.dto.response.TaskResponse;
import com.digitallifetwin.planning.entity.CalendarEvent;
import com.digitallifetwin.planning.entity.Task;
import com.digitallifetwin.planning.enums.TaskStatus;
import com.digitallifetwin.planning.exception.InvalidPlanningPeriodException;
import com.digitallifetwin.planning.mapper.PlanningMapper;
import com.digitallifetwin.planning.repository.CalendarEventRepository;
import com.digitallifetwin.planning.repository.TaskRepository;
import com.digitallifetwin.planning.service.ScheduleConflictService.TimeInterval;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DailyPlanService {

    private static final Duration TASK_LOOKBACK = Duration.ofDays(2);

    private final TaskRepository taskRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final ScheduleConflictService scheduleConflictService;
    private final PlanningMapper planningMapper;
    private final PlanningProperties planningProperties;

    @Transactional(readOnly = true)
    public DailyPlanResponse getDailyPlan(UUID userId, LocalDate date) {
        if (date == null) {
            throw new InvalidPlanningPeriodException("Date is required");
        }
        if (planningProperties.dayEnd().isBefore(planningProperties.dayStart())
                || planningProperties.dayEnd().equals(planningProperties.dayStart())) {
            throw new InvalidPlanningPeriodException("Configured planning day window is invalid");
        }

        ZoneId zone = planningProperties.zoneId();
        Instant dayStart = ZonedDateTime.of(date, planningProperties.dayStart(), zone).toInstant();
        Instant dayEnd = ZonedDateTime.of(date, planningProperties.dayEnd(), zone).toInstant();
        Instant lookback = dayStart.minus(TASK_LOOKBACK);

        List<Task> tasks = taskRepository.findDailyCandidates(userId, lookback, dayEnd).stream()
                .filter(task -> ScheduleConflictService.overlaps(
                        dayStart,
                        dayEnd,
                        task.getStartDateTime(),
                        ScheduleConflictService.taskEnd(task)))
                .toList();

        List<CalendarEvent> events = calendarEventRepository.findOverlappingRange(userId, dayStart, dayEnd);

        List<TaskResponse> taskResponses = new ArrayList<>();
        List<EventResponse> eventResponses = new ArrayList<>();

        for (Task task : tasks) {
            List<ScheduleConflictResponse> conflicts =
                    scheduleConflictService.findConflictsForTask(userId, task, task.getId()).stream()
                            .filter(conflict -> intersectsDay(conflict, tasks, events, dayStart, dayEnd))
                            .toList();
            taskResponses.add(planningMapper.toTaskResponse(task, conflicts));
        }

        for (CalendarEvent event : events) {
            List<ScheduleConflictResponse> conflicts =
                    scheduleConflictService.findConflictsForEvent(userId, event, event.getId()).stream()
                            .filter(conflict -> intersectsDay(conflict, tasks, events, dayStart, dayEnd))
                            .toList();
            eventResponses.add(planningMapper.toEventResponse(event, conflicts));
        }

        int plannedMinutes = tasks.stream()
                .mapToInt(Task::getPlannedDurationMinutes)
                .sum();
        int eventMinutes = events.stream()
                .mapToInt(event -> (int) Duration.between(event.getStartDateTime(), event.getEndDateTime()).toMinutes())
                .sum();

        List<TimeInterval> occupied = new ArrayList<>();
        for (Task task : tasks) {
            if (ScheduleConflictService.isActiveScheduledTask(task)
                    || task.getStatus() == TaskStatus.COMPLETED
                    || task.getStatus() == TaskStatus.IN_PROGRESS
                    || task.getStatus() == TaskStatus.PAUSED
                    || task.getStatus() == TaskStatus.OVERDUE
                    || task.getStatus() == TaskStatus.SCHEDULED) {
                occupied.add(new TimeInterval(task.getStartDateTime(), ScheduleConflictService.taskEnd(task)));
            }
        }
        for (CalendarEvent event : events) {
            occupied.add(new TimeInterval(event.getStartDateTime(), event.getEndDateTime()));
        }

        int occupiedMinutes = ScheduleConflictService.occupiedMinutes(occupied, dayStart, dayEnd);
        int availableMinutes = (int) Duration.between(dayStart, dayEnd).toMinutes();
        int freeMinutes = Math.max(0, availableMinutes - occupiedMinutes);
        int completedTasks = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        boolean overloaded = occupiedMinutes >= availableMinutes * planningProperties.overloadThresholdRatio();

        // Pairwise conflict edges are counted once via undirected pair keys.
        int conflictCount = countConflictPairs(tasks, events);

        DailyPlanSummaryResponse summary = new DailyPlanSummaryResponse(
                tasks.size(),
                completedTasks,
                plannedMinutes,
                eventMinutes,
                occupiedMinutes,
                freeMinutes,
                conflictCount,
                overloaded
        );

        return new DailyPlanResponse(
                date,
                zone.getId(),
                taskResponses,
                eventResponses,
                summary
        );
    }

    private int countConflictPairs(List<Task> tasks, List<CalendarEvent> events) {
        Set<String> pairs = new HashSet<>();
        for (int i = 0; i < tasks.size(); i++) {
            Task a = tasks.get(i);
            Instant aEnd = ScheduleConflictService.taskEnd(a);
            for (int j = i + 1; j < tasks.size(); j++) {
                Task b = tasks.get(j);
                if (ScheduleConflictService.overlaps(
                        a.getStartDateTime(), aEnd, b.getStartDateTime(), ScheduleConflictService.taskEnd(b))) {
                    pairs.add(pairKey("TASK", a.getId(), "TASK", b.getId()));
                }
            }
            for (CalendarEvent event : events) {
                if (ScheduleConflictService.overlaps(
                        a.getStartDateTime(), aEnd, event.getStartDateTime(), event.getEndDateTime())) {
                    pairs.add(pairKey("TASK", a.getId(), "EVENT", event.getId()));
                }
            }
        }
        for (int i = 0; i < events.size(); i++) {
            CalendarEvent a = events.get(i);
            for (int j = i + 1; j < events.size(); j++) {
                CalendarEvent b = events.get(j);
                if (ScheduleConflictService.overlaps(
                        a.getStartDateTime(), a.getEndDateTime(), b.getStartDateTime(), b.getEndDateTime())) {
                    pairs.add(pairKey("EVENT", a.getId(), "EVENT", b.getId()));
                }
            }
        }
        return pairs.size();
    }

    private String pairKey(String typeA, UUID idA, String typeB, UUID idB) {
        String left = typeA + ":" + idA;
        String right = typeB + ":" + idB;
        return left.compareTo(right) <= 0 ? left + "|" + right : right + "|" + left;
    }

    private boolean intersectsDay(
            ScheduleConflictResponse conflict,
            List<Task> tasks,
            List<CalendarEvent> events,
            Instant dayStart,
            Instant dayEnd) {
        if ("TASK".equals(conflict.conflictingResourceType())) {
            return tasks.stream().anyMatch(t -> t.getId().equals(conflict.conflictingResourceId()));
        }
        if ("EVENT".equals(conflict.conflictingResourceType())) {
            return events.stream().anyMatch(e -> e.getId().equals(conflict.conflictingResourceId()));
        }
        return false;
    }
}
