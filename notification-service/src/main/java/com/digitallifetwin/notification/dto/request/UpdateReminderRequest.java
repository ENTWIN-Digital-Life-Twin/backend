package com.digitallifetwin.notification.dto.request;

import com.digitallifetwin.notification.enums.RecurrenceType;
import com.digitallifetwin.notification.enums.ReminderSourceType;
import com.digitallifetwin.notification.enums.ReminderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record UpdateReminderRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 4000) String message,
        @NotNull ReminderType reminderType,
        @NotNull Instant triggerDateTime,
        boolean recurring,
        RecurrenceType recurrenceType,
        Boolean enabled,
        ReminderSourceType sourceType,
        UUID sourceResourceId,
        @Min(0) Integer advanceMinutes
) {
}
