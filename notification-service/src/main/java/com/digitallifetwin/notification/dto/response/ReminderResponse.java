package com.digitallifetwin.notification.dto.response;

import com.digitallifetwin.notification.enums.RecurrenceType;
import com.digitallifetwin.notification.enums.ReminderSourceType;
import com.digitallifetwin.notification.enums.ReminderType;
import java.time.Instant;
import java.util.UUID;

public record ReminderResponse(
        UUID id,
        UUID userId,
        String title,
        String message,
        ReminderType reminderType,
        Instant triggerDateTime,
        boolean recurring,
        RecurrenceType recurrenceType,
        boolean enabled,
        ReminderSourceType sourceType,
        UUID sourceResourceId,
        Integer advanceMinutes,
        Instant nextTriggerAt,
        Instant lastTriggeredAt,
        Instant createdAt,
        Instant updatedAt
) {
}
