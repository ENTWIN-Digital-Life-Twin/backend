package com.digitallifetwin.wellness.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateMoodRequest(
        @NotNull Instant recordedAt,
        @NotNull @Min(1) @Max(10) Integer moodLevel,
        @NotNull @Min(1) @Max(10) Integer stressLevel,
        @NotNull @Min(1) @Max(10) Integer fatigueLevel,
        @Size(max = 2000) String notes
) {
}
