package com.digitallifetwin.wellness.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateHealthRecordRequest(
        @NotNull Instant recordedAt,
        @Positive Double weightKg,
        @Min(30) @Max(250) Integer restingHeartRate,
        @Positive Integer systolicPressure,
        @Positive Integer diastolicPressure,
        @DecimalMin("30.0") @DecimalMax("45.0") Double temperatureCelsius,
        @Min(0) Integer stepCount,
        @Size(max = 2000) String notes
) {
}
