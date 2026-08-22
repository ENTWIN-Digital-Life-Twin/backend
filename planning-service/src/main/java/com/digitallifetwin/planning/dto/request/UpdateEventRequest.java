package com.digitallifetwin.planning.dto.request;

import com.digitallifetwin.planning.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateEventRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @NotNull(message = "Start date-time is required")
        Instant startDateTime,

        @NotNull(message = "End date-time is required")
        Instant endDateTime,

        boolean allDay,

        @NotNull(message = "Event type is required")
        EventType eventType,

        @Size(max = 255, message = "Location must be at most 255 characters")
        String locationLabel,

        boolean recurring,

        @Size(max = 255, message = "Recurrence rule must be at most 255 characters")
        String recurrenceRule
) {
}
