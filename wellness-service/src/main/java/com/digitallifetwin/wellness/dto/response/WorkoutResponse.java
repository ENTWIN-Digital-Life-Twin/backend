package com.digitallifetwin.wellness.dto.response;

import com.digitallifetwin.wellness.enums.ActivityType;
import com.digitallifetwin.wellness.enums.IntensityLevel;
import java.time.Instant;
import java.util.UUID;

public record WorkoutResponse(
        UUID id,
        UUID userId,
        ActivityType activityType,
        Instant startedAt,
        Integer durationMinutes,
        IntensityLevel intensity,
        Double caloriesBurned,
        Integer averageHeartRate,
        Double distanceKm,
        boolean completed,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
