package com.digitallifetwin.planning.service;

import com.digitallifetwin.planning.dto.SubtaskPayload;
import com.digitallifetwin.planning.dto.request.CreateTaskRequest;
import com.digitallifetwin.planning.dto.request.UpdateTaskRequest;
import com.digitallifetwin.planning.dto.request.UpdateTaskStatusRequest;
import com.digitallifetwin.planning.dto.response.PageResponse;
import com.digitallifetwin.planning.dto.response.ScheduleConflictResponse;
import com.digitallifetwin.planning.dto.response.TaskResponse;
import com.digitallifetwin.planning.entity.Task;
import com.digitallifetwin.planning.enums.TaskPriority;
import com.digitallifetwin.planning.enums.TaskStatus;
import com.digitallifetwin.planning.exception.TaskCategoryNotFoundException;
import com.digitallifetwin.planning.exception.TaskNotFoundException;
import com.digitallifetwin.planning.mapper.PlanningMapper;
import com.digitallifetwin.planning.repository.TaskCategoryRepository;
import com.digitallifetwin.planning.repository.TaskRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskCategoryRepository taskCategoryRepository;
    private final ScheduleConflictService scheduleConflictService;
    private final PlanningMapper planningMapper;

    @Transactional
    public TaskResponse create(UUID userId, CreateTaskRequest request) {
        validateTiming(request.startDateTime(), request.deadline());
        validateCategory(userId, request.categoryId());

        Task task = new Task();
        task.setUserId(userId);
        task.setTitle(request.title().trim());
        task.setDescription(trimToNull(request.description()));
        task.setCategoryId(request.categoryId());
        task.setPriority(request.priority());
        task.setPlannedDurationMinutes(request.plannedDurationMinutes());
        task.setStartDateTime(request.startDateTime());
        task.setDeadline(request.deadline());
        task.setCompletionPercentage(request.completionPercentage() != null ? request.completionPercentage() : 0);
        task.setEnergyRequired(request.energyRequired());
        task.setComplexityLevel(request.complexityLevel());
        task.setStatus(request.startDateTime() != null ? TaskStatus.SCHEDULED : TaskStatus.DRAFT);
        task.setDeleted(false);
        task.setSubtasks(normalizeSubtasks(request.subtasks()));

        Task saved = taskRepository.save(task);
        List<ScheduleConflictResponse> conflicts =
                scheduleConflictService.findConflictsForTask(userId, saved, saved.getId());
        return planningMapper.toTaskResponse(saved, conflicts);
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(UUID userId, UUID taskId) {
        Task task = requireOwned(userId, taskId);
        List<ScheduleConflictResponse> conflicts =
                scheduleConflictService.findConflictsForTask(userId, task, task.getId());
        return planningMapper.toTaskResponse(task, conflicts);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(
            UUID userId,
            TaskStatus status,
            TaskPriority priority,
            UUID categoryId,
            Instant from,
            Instant to,
            Pageable pageable) {
        Page<Task> page = taskRepository.findAll(
                TaskRepository.withFilters(userId, status, priority, categoryId, from, to),
                pageable);
        List<TaskResponse> content = page.getContent().stream()
                .map(planningMapper::toTaskResponse)
                .toList();
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Transactional
    public TaskResponse update(UUID userId, UUID taskId, UpdateTaskRequest request) {
        Task task = requireOwned(userId, taskId);
        validateTiming(request.startDateTime(), request.deadline());
        validateCategory(userId, request.categoryId());

        task.setTitle(request.title().trim());
        task.setDescription(trimToNull(request.description()));
        task.setCategoryId(request.categoryId());
        task.setPriority(request.priority());
        task.setPlannedDurationMinutes(request.plannedDurationMinutes());
        task.setActualDurationMinutes(request.actualDurationMinutes());
        task.setStartDateTime(request.startDateTime());
        task.setDeadline(request.deadline());
        task.setCompletionPercentage(request.completionPercentage());
        task.setEnergyRequired(request.energyRequired());
        task.setComplexityLevel(request.complexityLevel());
        task.setSubtasks(normalizeSubtasks(request.subtasks()));

        if (task.getStatus() == TaskStatus.DRAFT && request.startDateTime() != null) {
            task.setStatus(TaskStatus.SCHEDULED);
        }

        Task saved = taskRepository.save(task);
        List<ScheduleConflictResponse> conflicts =
                scheduleConflictService.findConflictsForTask(userId, saved, saved.getId());
        return planningMapper.toTaskResponse(saved, conflicts);
    }

    @Transactional
    public TaskResponse updateStatus(UUID userId, UUID taskId, UpdateTaskStatusRequest request) {
        Task task = requireOwned(userId, taskId);
        TaskStatusTransitions.assertAllowed(task.getStatus(), request.status());

        task.setStatus(request.status());
        if (request.status() == TaskStatus.COMPLETED) {
            task.setCompletionPercentage(100);
            task.setCompletedAt(Instant.now());
        } else if (task.getCompletedAt() != null && request.status() != TaskStatus.COMPLETED) {
            task.setCompletedAt(null);
        }

        Task saved = taskRepository.save(task);
        return planningMapper.toTaskResponse(saved);
    }

    @Transactional
    public void delete(UUID userId, UUID taskId) {
        Task task = requireOwned(userId, taskId);
        task.softDelete();
        taskRepository.save(task);
    }

    private Task requireOwned(UUID userId, UUID taskId) {
        return taskRepository.findByIdAndUserIdAndDeletedFalse(taskId, userId)
                .orElseThrow(TaskNotFoundException::new);
    }

    private void validateCategory(UUID userId, UUID categoryId) {
        if (categoryId == null) {
            return;
        }
        if (!taskCategoryRepository.isAccessibleToUser(categoryId, userId)) {
            throw new TaskCategoryNotFoundException("Task category not found or not accessible");
        }
    }

    private void validateTiming(Instant start, Instant deadline) {
        if (start != null && deadline != null && deadline.isBefore(start)) {
            throw new IllegalArgumentException("Deadline cannot be before start date-time");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<SubtaskPayload> normalizeSubtasks(List<SubtaskPayload> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }
        List<SubtaskPayload> normalized = new ArrayList<>();
        for (SubtaskPayload item : items) {
            if (normalized.size() >= 50) {
                break;
            }
            if (item == null || item.title() == null || item.title().isBlank()) {
                continue;
            }
            String title = item.title().trim();
            if (title.length() > 200) {
                title = title.substring(0, 200);
            }
            UUID id = item.id() != null ? item.id() : UUID.randomUUID();
            normalized.add(new SubtaskPayload(id, title, item.done()));
        }
        return normalized;
    }
}
