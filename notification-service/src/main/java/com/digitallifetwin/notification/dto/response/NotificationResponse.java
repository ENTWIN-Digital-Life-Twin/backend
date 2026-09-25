package com.digitallifetwin.notification.dto.response;

import com.digitallifetwin.notification.enums.NotificationChannel;
import com.digitallifetwin.notification.enums.NotificationStatus;
import com.digitallifetwin.notification.enums.NotificationType;
import com.digitallifetwin.notification.enums.ReminderSourceType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        NotificationType notificationType,
        String title,
        String message,
        NotificationChannel channel,
        NotificationStatus status,
        Instant scheduledAt,
        Instant sentAt,
        Instant readAt,
        int retryCount,
        UUID reminderId,
        ReminderSourceType sourceType,
        UUID sourceResourceId,
        Instant createdAt,
        Instant updatedAt
) {
}
