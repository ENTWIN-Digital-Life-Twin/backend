package com.digitallifetwin.auth.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ContactMessageResponse(
        UUID id,
        String name,
        String email,
        String subject,
        String message,
        Instant createdAt
) {
}
