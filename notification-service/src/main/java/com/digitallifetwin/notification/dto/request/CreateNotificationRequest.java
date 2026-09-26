package com.digitallifetwin.notification.dto.request;

import com.digitallifetwin.notification.enums.NotificationType;
import com.digitallifetwin.notification.enums.ReminderSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateNotificationRequest(
        @NotNull NotificationType notificationType,
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 4000) String message,
        ReminderSourceType sourceType
) {
}
