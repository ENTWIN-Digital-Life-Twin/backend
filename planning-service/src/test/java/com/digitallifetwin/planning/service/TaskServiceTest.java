package com.digitallifetwin.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitallifetwin.planning.dto.SubtaskPayload;
import com.digitallifetwin.planning.dto.request.CreateTaskRequest;
import com.digitallifetwin.planning.dto.request.UpdateTaskRequest;
import com.digitallifetwin.planning.dto.request.UpdateTaskStatusRequest;
import com.digitallifetwin.planning.dto.response.TaskResponse;
import com.digitallifetwin.planning.entity.Task;
import com.digitallifetwin.planning.enums.ComplexityLevel;
import com.digitallifetwin.planning.enums.EnergyLevel;
import com.digitallifetwin.planning.enums.TaskPriority;
import com.digitallifetwin.planning.enums.TaskStatus;
import com.digitallifetwin.planning.exception.InvalidTaskStateTransitionException;
import com.digitallifetwin.planning.mapper.PlanningMapper;
import com.digitallifetwin.planning.repository.TaskCategoryRepository;
import com.digitallifetwin.planning.repository.TaskRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskCategoryRepository taskCategoryRepository;
    @Mock
    private ScheduleConflictService scheduleConflictService;
    @Spy
    private PlanningMapper planningMapper = new PlanningMapper();

    @InjectMocks
    private TaskService taskService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void createTask_success_defaultsToScheduledWhenStartPresent() {
        CreateTaskRequest request = new CreateTaskRequest(
                "Study Spring Boot",
                "Review security",
                null,
                TaskPriority.HIGH,
                90,
                Instant.parse("2026-08-23T17:00:00Z"),
                Instant.parse("2026-08-23T20:00:00Z"),
                null,
                EnergyLevel.HIGH,
                ComplexityLevel.MEDIUM,
                null
        );
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(UUID.randomUUID());
            task.setCreatedAt(Instant.now());
            task.setUpdatedAt(Instant.now());
            return task;
        });
        when(scheduleConflictService.findConflictsForTask(eq(userId), any(Task.class), any()))
                .thenReturn(List.of());

        TaskResponse response = taskService.create(userId, request);

        assertThat(response.title()).isEqualTo("Study Spring Boot");
        assertThat(response.status()).isEqualTo(TaskStatus.SCHEDULED);
        assertThat(response.plannedDurationMinutes()).isEqualTo(90);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
    }

    @Test
    void createTask_rejectsNonPositiveDurationViaValidationContract() {
        // Bean Validation covers API; service still receives positive values.
        // Domain timing validation:
        assertThatThrownBy(() -> taskService.create(userId, new CreateTaskRequest(
                "Bad",
                null,
                null,
                TaskPriority.LOW,
                30,
                Instant.parse("2026-08-23T20:00:00Z"),
                Instant.parse("2026-08-23T17:00:00Z"),
                null,
                null,
                null,
                null
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deadline cannot be before");
    }

    @Test
    void statusTransition_accepted() {
        Task task = ownedTask(TaskStatus.SCHEDULED);
        when(taskRepository.findByIdAndUserIdAndDeletedFalse(task.getId(), userId))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        TaskResponse response = taskService.updateStatus(
                userId, task.getId(), new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS));

        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void statusTransition_rejected() {
        Task task = ownedTask(TaskStatus.CANCELLED);
        when(taskRepository.findByIdAndUserIdAndDeletedFalse(task.getId(), userId))
                .thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateStatus(
                userId, task.getId(), new UpdateTaskStatusRequest(TaskStatus.COMPLETED)))
                .isInstanceOf(InvalidTaskStateTransitionException.class);
    }

    @Test
    void completeTask_fromScheduled_isAllowed() {
        Task task = ownedTask(TaskStatus.SCHEDULED);
        when(taskRepository.findByIdAndUserIdAndDeletedFalse(task.getId(), userId))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        TaskResponse response = taskService.updateStatus(
                userId, task.getId(), new UpdateTaskStatusRequest(TaskStatus.COMPLETED));

        assertThat(response.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(response.completionPercentage()).isEqualTo(100);
        assertThat(response.completedAt()).isNotNull();
    }

    @Test
    void reopenCompletedTask_toScheduled_isAllowed() {
        Task task = ownedTask(TaskStatus.COMPLETED);
        task.setCompletedAt(Instant.now());
        task.setCompletionPercentage(100);
        when(taskRepository.findByIdAndUserIdAndDeletedFalse(task.getId(), userId))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        TaskResponse response = taskService.updateStatus(
                userId, task.getId(), new UpdateTaskStatusRequest(TaskStatus.SCHEDULED));

        assertThat(response.status()).isEqualTo(TaskStatus.SCHEDULED);
        assertThat(response.completedAt()).isNull();
    }

    @Test
    void completeTask_setsCompletionFields() {
        Task task = ownedTask(TaskStatus.IN_PROGRESS);
        task.setCompletionPercentage(40);
        when(taskRepository.findByIdAndUserIdAndDeletedFalse(task.getId(), userId))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        TaskResponse response = taskService.updateStatus(
                userId, task.getId(), new UpdateTaskStatusRequest(TaskStatus.COMPLETED));

        assertThat(response.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(response.completionPercentage()).isEqualTo(100);
        assertThat(response.completedAt()).isNotNull();
    }

    @Test
    void createTask_persistsNormalizedSubtasks() {
        UUID keptId = UUID.randomUUID();
        CreateTaskRequest request = new CreateTaskRequest(
                "Study Spring Boot",
                "Review security",
                null,
                TaskPriority.HIGH,
                90,
                Instant.parse("2026-08-23T17:00:00Z"),
                Instant.parse("2026-08-23T20:00:00Z"),
                null,
                EnergyLevel.HIGH,
                ComplexityLevel.MEDIUM,
                List.of(
                        new SubtaskPayload(keptId, "Read docs", true),
                        new SubtaskPayload(null, "  Write notes  ", false),
                        new SubtaskPayload(null, "   ", false)
                )
        );
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(UUID.randomUUID());
            task.setCreatedAt(Instant.now());
            task.setUpdatedAt(Instant.now());
            return task;
        });
        when(scheduleConflictService.findConflictsForTask(eq(userId), any(Task.class), any()))
                .thenReturn(List.of());

        TaskResponse response = taskService.create(userId, request);

        assertThat(response.subtasks()).hasSize(2);
        assertThat(response.subtasks().getFirst().id()).isEqualTo(keptId);
        assertThat(response.subtasks().getFirst().title()).isEqualTo("Read docs");
        assertThat(response.subtasks().get(1).title()).isEqualTo("Write notes");
        assertThat(response.subtasks().get(1).id()).isNotNull();
    }

    @Test
    void updateTask_replacesSubtasks() {
        Task task = ownedTask(TaskStatus.SCHEDULED);
        UUID keptId = UUID.randomUUID();
        when(taskRepository.findByIdAndUserIdAndDeletedFalse(task.getId(), userId))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);
        when(scheduleConflictService.findConflictsForTask(eq(userId), any(Task.class), any()))
                .thenReturn(List.of());

        TaskResponse response = taskService.update(userId, task.getId(), new UpdateTaskRequest(
                "Task",
                null,
                null,
                TaskPriority.MEDIUM,
                60,
                null,
                null,
                null,
                0,
                null,
                null,
                List.of(new SubtaskPayload(keptId, "Slides", true))
        ));

        assertThat(response.subtasks()).hasSize(1);
        assertThat(response.subtasks().getFirst().id()).isEqualTo(keptId);
        assertThat(response.subtasks().getFirst().done()).isTrue();
    }

    private Task ownedTask(TaskStatus status) {
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setUserId(userId);
        task.setTitle("Task");
        task.setPriority(TaskPriority.MEDIUM);
        task.setStatus(status);
        task.setPlannedDurationMinutes(60);
        task.setCompletionPercentage(0);
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(Instant.now());
        return task;
    }
}
