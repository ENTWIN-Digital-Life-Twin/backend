package com.digitallifetwin.planning.dto.response;

import java.util.UUID;

public record UpcomingEventResponse(
        UUID id,
        String time,
        String title,
        String location,
        String eventType
) {
}
