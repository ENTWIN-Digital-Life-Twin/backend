package com.digitallifetwin.wellness.dto.response;

import java.time.Instant;
import java.util.UUID;

public record HealthRecordResponse(
        UUID id,
        UUID userId,
        Instant recordedAt,
        Double weightKg,
        Integer restingHeartRate,
        Integer systolicPressure,
        Integer diastolicPressure,
        Double temperatureCelsius,
        Integer stepCount,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
