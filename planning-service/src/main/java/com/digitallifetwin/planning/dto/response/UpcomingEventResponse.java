package com.digitallifetwin.planning.dto.response;

import java.util.List;
import java.util.UUID;

public record UpcomingEventResponse(
        UUID id,
        String time,
        String title,
        String location,
        Boolean isOnline,
        List<String> participants,
        String eventType
) {
}
