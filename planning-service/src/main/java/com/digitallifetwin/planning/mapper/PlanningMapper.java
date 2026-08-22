package com.digitallifetwin.planning.mapper;

import com.digitallifetwin.planning.dto.response.CategoryResponse;
import com.digitallifetwin.planning.dto.response.EventResponse;
import com.digitallifetwin.planning.dto.response.ScheduleConflictResponse;
import com.digitallifetwin.planning.dto.response.TaskResponse;
import com.digitallifetwin.planning.entity.CalendarEvent;
import com.digitallifetwin.planning.entity.Task;
import com.digitallifetwin.planning.entity.TaskCategory;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PlanningMapper {

    public TaskResponse toTaskResponse(Task task) {
        return toTaskResponse(task, List.of());
    }

    public TaskResponse toTaskResponse(Task task, List<ScheduleConflictResponse> conflicts) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getCategoryId(),
                task.getPriority(),
                task.getStatus(),
                task.getPlannedDurationMinutes(),
                task.getActualDurationMinutes(),
                task.getStartDateTime(),
                task.getDeadline(),
                task.getCompletionPercentage(),
                task.getEnergyRequired(),
                task.getComplexityLevel(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getCompletedAt(),
                conflicts
        );
    }

    public EventResponse toEventResponse(CalendarEvent event) {
        return toEventResponse(event, List.of());
    }

    public EventResponse toEventResponse(CalendarEvent event, List<ScheduleConflictResponse> conflicts) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getStartDateTime(),
                event.getEndDateTime(),
                event.isAllDay(),
                event.getEventType(),
                event.getLocationLabel(),
                event.isRecurring(),
                event.getRecurrenceRule(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                conflicts
        );
    }

    public CategoryResponse toCategoryResponse(TaskCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getColorCode(),
                category.isSystemCategory(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
