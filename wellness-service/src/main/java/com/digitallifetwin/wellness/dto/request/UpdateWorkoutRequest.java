package com.digitallifetwin.wellness.dto.request;

import com.digitallifetwin.wellness.enums.ActivityType;
import com.digitallifetwin.wellness.enums.IntensityLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateWorkoutRequest(
        @NotNull ActivityType activityType,
        @NotNull Instant startedAt,
        @NotNull @Min(1) Integer durationMinutes,
        @NotNull IntensityLevel intensity,
        @DecimalMin("0") Double caloriesBurned,
        @Min(30) @Max(250) Integer averageHeartRate,
        @DecimalMin("0") Double distanceKm,
        Boolean completed,
        @Size(max = 2000) String notes
) {
}
