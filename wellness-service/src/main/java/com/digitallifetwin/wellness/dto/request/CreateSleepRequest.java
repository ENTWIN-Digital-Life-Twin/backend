package com.digitallifetwin.wellness.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateSleepRequest(
        @NotNull Instant sleepStart,
        @NotNull Instant wakeTime,
        @Min(1) @Max(10) Integer qualityScore,
        @Min(0) Integer interruptions,
        @Size(max = 2000) String notes
) {
}
