package com.digitallifetwin.wellness.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SleepResponse(
        UUID id,
        UUID userId,
        Instant sleepStart,
        Instant wakeTime,
        Integer durationMinutes,
        Integer qualityScore,
        Integer interruptions,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
