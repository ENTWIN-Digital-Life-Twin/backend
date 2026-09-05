package com.digitallifetwin.notification.mapper;

import com.digitallifetwin.notification.dto.response.NotificationResponse;
import com.digitallifetwin.notification.dto.response.ReminderResponse;
import com.digitallifetwin.notification.entity.Notification;
import com.digitallifetwin.notification.entity.Reminder;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public ReminderResponse toReminderResponse(Reminder reminder) {
        return new ReminderResponse(
                reminder.getId(),
                reminder.getUserId(),
                reminder.getTitle(),
                reminder.getMessage(),
                reminder.getReminderType(),
                reminder.getTriggerDateTime(),
                reminder.isRecurring(),
                reminder.getRecurrenceType(),
                reminder.isEnabled(),
                reminder.getSourceType(),
                reminder.getSourceResourceId(),
                reminder.getAdvanceMinutes(),
                reminder.getNextTriggerAt(),
                reminder.getLastTriggeredAt(),
                reminder.getCreatedAt(),
                reminder.getUpdatedAt()
        );
    }

    public NotificationResponse toNotificationResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getChannel(),
                notification.getStatus(),
                notification.getScheduledAt(),
                notification.getSentAt(),
                notification.getReadAt(),
                notification.getRetryCount(),
                notification.getReminderId(),
                notification.getSourceType(),
                notification.getSourceResourceId(),
                notification.getCreatedAt(),
                notification.getUpdatedAt()
        );
    }
}
