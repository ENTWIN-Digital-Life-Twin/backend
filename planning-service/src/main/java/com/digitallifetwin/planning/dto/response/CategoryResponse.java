package com.digitallifetwin.planning.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String description,
        String colorCode,
        boolean systemCategory,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
