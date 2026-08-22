package com.digitallifetwin.planning.dto.response;

import com.digitallifetwin.planning.enums.EventType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String title,
        String description,
        Instant startDateTime,
        Instant endDateTime,
        boolean allDay,
        EventType eventType,
        String locationLabel,
        boolean recurring,
        String recurrenceRule,
        Instant createdAt,
        Instant updatedAt,
        List<ScheduleConflictResponse> conflicts
) {
}
