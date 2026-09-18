package com.digitallifetwin.planning.dto.request;

import com.digitallifetwin.planning.dto.SubtaskPayload;
import com.digitallifetwin.planning.enums.ComplexityLevel;
import com.digitallifetwin.planning.enums.EnergyLevel;
import com.digitallifetwin.planning.enums.TaskPriority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateTaskRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        UUID categoryId,

        @NotNull(message = "Priority is required")
        TaskPriority priority,

        @NotNull(message = "Planned duration is required")
        @Positive(message = "Planned duration must be positive")
        Integer plannedDurationMinutes,

        Instant startDateTime,

        Instant deadline,

        @Min(value = 0, message = "Completion percentage must be at least 0")
        @Max(value = 100, message = "Completion percentage must be at most 100")
        Integer completionPercentage,

        EnergyLevel energyRequired,

        ComplexityLevel complexityLevel,

        List<SubtaskPayload> subtasks
) {
}
