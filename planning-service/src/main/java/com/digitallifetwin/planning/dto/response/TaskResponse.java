package com.digitallifetwin.planning.dto.response;

import com.digitallifetwin.planning.enums.ComplexityLevel;
import com.digitallifetwin.planning.enums.EnergyLevel;
import com.digitallifetwin.planning.enums.TaskPriority;
import com.digitallifetwin.planning.enums.TaskStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String description,
        UUID categoryId,
        TaskPriority priority,
        TaskStatus status,
        Integer plannedDurationMinutes,
        Integer actualDurationMinutes,
        Instant startDateTime,
        Instant deadline,
        Integer completionPercentage,
        EnergyLevel energyRequired,
        ComplexityLevel complexityLevel,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt,
        List<ScheduleConflictResponse> conflicts
) {
}
