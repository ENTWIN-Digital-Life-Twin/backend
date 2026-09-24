package com.digitallifetwin.planning.service;

import com.digitallifetwin.planning.dto.response.DashboardStatsResponse;
import com.digitallifetwin.planning.dto.response.TimelineEventResponse;
import com.digitallifetwin.planning.dto.response.UpcomingEventResponse;
import com.digitallifetwin.planning.entity.CalendarEvent;
import com.digitallifetwin.planning.entity.Task;
import com.digitallifetwin.planning.enums.TaskStatus;
import com.digitallifetwin.planning.repository.CalendarEventRepository;
import com.digitallifetwin.planning.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TaskRepository taskRepository;
    private final CalendarEventRepository eventRepository;

    /**
     * Get dashboard statistics for today
     */
    public DashboardStatsResponse getStats(UUID userId) {
        LocalDate today = LocalDate.now();
        Instant startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        // Get today's tasks
        List<Task> todayTasks = taskRepository.findByUserIdAndDueDateBetween(userId, startOfDay, endOfDay);
        
        int totalTasks = todayTasks.size();
        int completedTasks = (int) todayTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .count();
        
        int productivityPercent = totalTasks > 0 ? (completedTasks * 100) / totalTasks : 0;
        
        // Calculate focus time (sum of completed task durations)
        long totalMinutes = todayTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .mapToLong(task -> 30) // Assume 30 min per task, or use actual duration if available
                .sum();
        
        String focusTime = formatDuration(totalMinutes);
        
        // Calculate breaks (assume 1 break per 2 hours of work)
        int breaksTaken = (int) (totalMinutes / 120);
        
        // Calculate goals met (tasks with high priority completed)
        long highPriorityTasks = todayTasks.stream()
                .filter(task -> task.getPriority() != null)
                .count();
        long highPriorityCompleted = todayTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED && task.getPriority() != null)
                .count();
        
        int goalsMetPercent = highPriorityTasks > 0 ? (int) ((highPriorityCompleted * 100) / highPriorityTasks) : 0;
        
        // AI confidence (placeholder - would come from AI service)
        int aiConfidence = 82;
        
        // Calculate free time
        long workMinutes = totalMinutes + (breaksTaken * 15);
        long freeMinutes = (16 * 60) - workMinutes; // Assume 16-hour day
        long eveningFree = Math.min(freeMinutes, 150); // Max 2.5 hours evening
        long lunchFree = 45; // Standard lunch break
        
        return DashboardStatsResponse.builder()
                .productivityPercent(productivityPercent)
                .tasksCompleted(completedTasks)
                .tasksTotal(totalTasks)
                .focusTime(focusTime)
                .breaksTaken(breaksTaken)
                .goalsMetPercent(goalsMetPercent)
                .aiConfidence(aiConfidence)
                .freeTimeTotal(formatDuration(freeMinutes))
                .freeTimeEvening(formatDuration(eveningFree))
                .freeTimeLunch(formatDuration(lunchFree))
                .build();
    }

    /**
     * Get today's timeline from tasks and events
     */
    public List<TimelineEventResponse> getTimeline(UUID userId) {
        LocalDate today = LocalDate.now();
        Instant startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<TimelineEventResponse> timeline = new ArrayList<>();
        
        // Get today's tasks
        List<Task> todayTasks = taskRepository.findByUserIdAndDueDateBetween(userId, startOfDay, endOfDay);
        
        // Get today's events
        List<CalendarEvent> todayEvents = eventRepository.findByUserIdAndStartTimeBetween(userId, startOfDay, endOfDay);
        
        // Convert tasks to timeline events
        for (Task task : todayTasks) {
            LocalTime time = task.getDeadline() != null 
                ? LocalDateTime.ofInstant(task.getDeadline(), ZoneId.systemDefault()).toLocalTime()
                : LocalTime.of(9, 0); // Default to 9 AM
            
            timeline.add(TimelineEventResponse.builder()
                    .time(time.format(DateTimeFormatter.ofPattern("HH:mm")))
                    .title(task.getTitle())
                    .detail(task.getDescription() != null ? task.getDescription() : "Task")
                    .type("work")
                    .build());
        }
        
        // Convert calendar events to timeline events
        for (CalendarEvent event : todayEvents) {
            LocalTime time = LocalDateTime.ofInstant(event.getStartDateTime(), ZoneId.systemDefault()).toLocalTime();
            
            String type = event.getEventType() != null ? event.getEventType().name().toLowerCase() : "meeting";
            
            timeline.add(TimelineEventResponse.builder()
                    .time(time.format(DateTimeFormatter.ofPattern("HH:mm")))
                    .title(event.getTitle())
                    .detail(event.getDescription() != null ? event.getDescription() : "Event")
                    .type(type)
                    .build());
        }
        
        // Sort by time
        timeline.sort((a, b) -> a.getTime().compareTo(b.getTime()));
        
        return timeline;
    }

    /**
     * Get the next upcoming event
     */
    public UpcomingEventResponse getUpcomingEvent(UUID userId) {
        Instant now = Instant.now();
        Instant endOfDay = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        // Find next event
        List<CalendarEvent> upcomingEvents = eventRepository.findByUserIdAndStartTimeBetween(userId, now, endOfDay);
        
        if (upcomingEvents.isEmpty()) {
            return null;
        }
        
        // Get the first upcoming event
        CalendarEvent event = upcomingEvents.stream()
                .min((a, b) -> a.getStartDateTime().compareTo(b.getStartDateTime()))
                .orElse(null);
        
        if (event == null) {
            return null;
        }
        
        LocalDateTime startTime = LocalDateTime.ofInstant(event.getStartDateTime(), ZoneId.systemDefault());
        LocalDateTime endTime = event.getEndDateTime() != null 
            ? LocalDateTime.ofInstant(event.getEndDateTime(), ZoneId.systemDefault())
            : startTime.plusHours(1);
        
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String timeRange = startTime.format(timeFormatter) + " - " + endTime.format(timeFormatter);
        
        // Extract participants from description or use placeholder
        List<String> participants = new ArrayList<>();
        if (event.getDescription() != null && event.getDescription().contains("participants:")) {
            // Parse participants from description
            String[] parts = event.getDescription().split("participants:");
            if (parts.length > 1) {
                String[] names = parts[1].split(",");
                for (String name : names) {
                    participants.add(name.trim());
                }
            }
        }
        
        return UpcomingEventResponse.builder()
                .time(timeRange)
                .title(event.getTitle())
                .location(event.getLocationLabel() != null ? event.getLocationLabel() : "TBD")
                .isOnline(event.getLocationLabel() != null && event.getLocationLabel().toLowerCase().contains("online"))
                .participants(participants)
                .build();
    }

    /**
     * Format duration in minutes to "Xh Ym" format
     */
    private String formatDuration(long minutes) {
        if (minutes < 60) {
            return minutes + "m";
        }
        long hours = minutes / 60;
        long mins = minutes % 60;
        return hours + "h " + mins + "m";
    }
}
