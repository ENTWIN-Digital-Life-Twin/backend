package com.digitallifetwin.wellness.dto.response;

import com.digitallifetwin.wellness.enums.GoalStatus;
import com.digitallifetwin.wellness.enums.WellnessGoalType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record WellnessGoalResponse(
        UUID id,
        UUID userId,
        WellnessGoalType goalType,
        Double targetValue,
        Double currentValue,
        String unit,
        LocalDate startDate,
        LocalDate targetDate,
        GoalStatus status,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
