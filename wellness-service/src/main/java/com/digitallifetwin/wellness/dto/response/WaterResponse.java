package com.digitallifetwin.wellness.dto.response;

import com.digitallifetwin.wellness.enums.BeverageType;
import java.time.Instant;
import java.util.UUID;

public record WaterResponse(
        UUID id,
        UUID userId,
        Integer quantityMl,
        Instant consumedAt,
        BeverageType beverageType,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
