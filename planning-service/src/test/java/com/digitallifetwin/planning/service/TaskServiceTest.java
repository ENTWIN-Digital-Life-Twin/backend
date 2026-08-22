package com.digitallifetwin.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitallifetwin.planning.dto.request.CreateTaskRequest;
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
                ComplexityLevel.MEDIUM
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
        Task task = ownedTask(TaskStatus.COMPLETED);
        when(taskRepository.findByIdAndUserIdAndDeletedFalse(task.getId(), userId))
                .thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateStatus(
                userId, task.getId(), new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS)))
                .isInstanceOf(InvalidTaskStateTransitionException.class);
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
