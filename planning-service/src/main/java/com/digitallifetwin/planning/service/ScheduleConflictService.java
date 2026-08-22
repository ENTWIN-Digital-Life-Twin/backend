package com.digitallifetwin.planning.service;

import com.digitallifetwin.planning.dto.response.ScheduleConflictResponse;
import com.digitallifetwin.planning.entity.CalendarEvent;
import com.digitallifetwin.planning.entity.Task;
import com.digitallifetwin.planning.enums.TaskStatus;
import com.digitallifetwin.planning.repository.CalendarEventRepository;
import com.digitallifetwin.planning.repository.TaskRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleConflictService {

    private static final Duration MAX_TASK_LOOKBACK = Duration.ofDays(2);

    private final TaskRepository taskRepository;
    private final CalendarEventRepository calendarEventRepository;

    @Transactional(readOnly = true)
    public List<ScheduleConflictResponse> findConflictsForTask(
            UUID userId, Task candidate, UUID excludeTaskId) {
        if (candidate.getStartDateTime() == null || candidate.getPlannedDurationMinutes() == null) {
            return List.of();
        }
        Instant start = candidate.getStartDateTime();
        Instant end = start.plus(Duration.ofMinutes(candidate.getPlannedDurationMinutes()));
        return findConflicts(userId, start, end, excludeTaskId, null);
    }

    @Transactional(readOnly = true)
    public List<ScheduleConflictResponse> findConflictsForEvent(
            UUID userId, CalendarEvent candidate, UUID excludeEventId) {
        return findConflicts(
                userId,
                candidate.getStartDateTime(),
                candidate.getEndDateTime(),
                null,
                excludeEventId
        );
    }

    @Transactional(readOnly = true)
    public List<ScheduleConflictResponse> findConflicts(
            UUID userId,
            Instant rangeStart,
            Instant rangeEnd,
            UUID excludeTaskId,
            UUID excludeEventId) {
        List<ScheduleConflictResponse> conflicts = new ArrayList<>();

        Instant lookback = rangeStart.minus(MAX_TASK_LOOKBACK);
        for (Task task : taskRepository.findScheduledCandidates(userId, lookback, rangeEnd)) {
            if (excludeTaskId != null && Objects.equals(task.getId(), excludeTaskId)) {
                continue;
            }
            if (!isActiveScheduledTask(task)) {
                continue;
            }
            Instant taskEnd = taskEnd(task);
            if (overlaps(rangeStart, rangeEnd, task.getStartDateTime(), taskEnd)) {
                conflicts.add(new ScheduleConflictResponse(
                        "OVERLAP",
                        task.getId(),
                        "TASK",
                        "Overlaps with task: " + task.getTitle()
                ));
            }
        }

        for (CalendarEvent event : calendarEventRepository.findOverlappingRange(userId, rangeStart, rangeEnd)) {
            if (excludeEventId != null && Objects.equals(event.getId(), excludeEventId)) {
                continue;
            }
            if (overlaps(rangeStart, rangeEnd, event.getStartDateTime(), event.getEndDateTime())) {
                conflicts.add(new ScheduleConflictResponse(
                        "OVERLAP",
                        event.getId(),
                        "EVENT",
                        "Overlaps with event: " + event.getTitle()
                ));
            }
        }
        return conflicts;
    }

    public static boolean overlaps(Instant aStart, Instant aEnd, Instant bStart, Instant bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    public static Instant taskEnd(Task task) {
        return task.getStartDateTime().plus(Duration.ofMinutes(task.getPlannedDurationMinutes()));
    }

    public static boolean isActiveScheduledTask(Task task) {
        return task.getStartDateTime() != null
                && task.getStatus() != TaskStatus.CANCELLED
                && task.getStatus() != TaskStatus.COMPLETED
                && task.getStatus() != TaskStatus.DRAFT;
    }

    /**
     * Merges occupied intervals and returns total occupied minutes within [windowStart, windowEnd].
     */
    public static int occupiedMinutes(List<TimeInterval> intervals, Instant windowStart, Instant windowEnd) {
        List<TimeInterval> clipped = intervals.stream()
                .map(interval -> interval.clip(windowStart, windowEnd))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TimeInterval::start))
                .toList();

        if (clipped.isEmpty()) {
            return 0;
        }

        List<TimeInterval> merged = new ArrayList<>();
        TimeInterval current = clipped.getFirst();
        for (int i = 1; i < clipped.size(); i++) {
            TimeInterval next = clipped.get(i);
            if (!current.end().isAfter(next.start())) {
                merged.add(current);
                current = next;
            } else if (next.end().isAfter(current.end())) {
                current = new TimeInterval(current.start(), next.end());
            }
        }
        merged.add(current);

        long seconds = merged.stream()
                .mapToLong(interval -> Duration.between(interval.start(), interval.end()).getSeconds())
                .sum();
        return (int) (seconds / 60);
    }

    public record TimeInterval(Instant start, Instant end) {

        public TimeInterval clip(Instant windowStart, Instant windowEnd) {
            Instant clippedStart = start.isBefore(windowStart) ? windowStart : start;
            Instant clippedEnd = end.isAfter(windowEnd) ? windowEnd : end;
            if (!clippedStart.isBefore(clippedEnd)) {
                return null;
            }
            return new TimeInterval(clippedStart, clippedEnd);
        }
    }
}
