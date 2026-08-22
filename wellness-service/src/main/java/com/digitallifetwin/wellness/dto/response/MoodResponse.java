package com.digitallifetwin.wellness.dto.response;

import java.time.Instant;
import java.util.UUID;

public record MoodResponse(
        UUID id,
        UUID userId,
        Instant recordedAt,
        Integer moodLevel,
        Integer stressLevel,
        Integer fatigueLevel,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
