package com.digitallifetwin.notification.service;

import com.digitallifetwin.notification.entity.Notification;
import com.digitallifetwin.notification.entity.Reminder;
import com.digitallifetwin.notification.enums.NotificationChannel;
import com.digitallifetwin.notification.enums.NotificationStatus;
import com.digitallifetwin.notification.enums.NotificationType;
import com.digitallifetwin.notification.enums.RecurrenceType;
import com.digitallifetwin.notification.repository.NotificationRepository;
import com.digitallifetwin.notification.repository.ReminderRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processes due reminders with a transactional claim on {@code nextTriggerAt}.
 *
 * <p>Duplicate prevention: {@code SELECT ... FOR UPDATE SKIP LOCKED} on the reminder row,
 * then in the same transaction create a SENT in-app notification and either disable
 * (one-time) or advance {@code nextTriggerAt} (DAILY +1 day, WEEKLY +7 days). A later
 * scheduler tick no longer sees the same due instant.
 */
@Slf4j
@Service
public class ReminderExecutionService {

    static final int BATCH_SIZE = 100;

    private final ReminderRepository reminderRepository;
    private final NotificationRepository notificationRepository;
    private final ReminderExecutionService self;

    public ReminderExecutionService(
            ReminderRepository reminderRepository,
            NotificationRepository notificationRepository,
            @Lazy ReminderExecutionService self) {
        this.reminderRepository = reminderRepository;
        this.notificationRepository = notificationRepository;
        this.self = self;
    }

    public int processDueReminders(Instant now) {
        List<UUID> dueIds = reminderRepository.findDueIds(now, PageRequest.of(0, BATCH_SIZE));
        int processed = 0;
        for (UUID reminderId : dueIds) {
            try {
                if (delegate().processOne(reminderId, now)) {
                    processed++;
                }
            } catch (RuntimeException ex) {
                log.warn("Failed to process reminder id={}", reminderId);
                try {
                    delegate().recordFailure(reminderId, now);
                } catch (RuntimeException recordEx) {
                    log.warn("Could not record failure for reminder id={}", reminderId);
                }
            }
        }
        return processed;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processOne(UUID reminderId, Instant now) {
        Reminder reminder = reminderRepository.findByIdForUpdate(reminderId).orElse(null);
        if (reminder == null || reminder.isDeleted() || !reminder.isEnabled()) {
            return false;
        }
        if (reminder.getNextTriggerAt() == null || reminder.getNextTriggerAt().isAfter(now)) {
            return false;
        }

        Instant claimedTrigger = reminder.getNextTriggerAt();
        Notification notification = new Notification();
        notification.setUserId(reminder.getUserId());
        notification.setNotificationType(NotificationType.REMINDER);
        notification.setTitle(reminder.getTitle());
        notification.setMessage(reminder.getMessage() == null || reminder.getMessage().isBlank()
                ? reminder.getTitle()
                : reminder.getMessage());
        notification.setChannel(NotificationChannel.IN_APP);
        notification.setStatus(NotificationStatus.SENT);
        notification.setScheduledAt(claimedTrigger);
        notification.setSentAt(now);
        notification.setRetryCount(0);
        notification.setReminderId(reminder.getId());
        notification.setSourceType(reminder.getSourceType());
        notification.setSourceResourceId(reminder.getSourceResourceId());
        notification.setDeleted(false);
        notificationRepository.save(notification);

        reminder.setLastTriggeredAt(now);
        if (reminder.isRecurring() && reminder.getRecurrenceType() != RecurrenceType.NONE) {
            reminder.setNextTriggerAt(ReminderRecurrence.advance(claimedTrigger, reminder.getRecurrenceType()));
        } else {
            reminder.setEnabled(false);
        }
        reminderRepository.save(reminder);
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID reminderId, Instant now) {
        Reminder reminder = reminderRepository.findById(reminderId).orElse(null);
        if (reminder == null || reminder.isDeleted()) {
            return;
        }
        long previousFailures = notificationRepository.countByUserIdAndReminderIdAndDeletedFalse(
                reminder.getUserId(), reminder.getId());
        Notification failed = new Notification();
        failed.setUserId(reminder.getUserId());
        failed.setNotificationType(NotificationType.REMINDER);
        failed.setTitle(reminder.getTitle());
        failed.setMessage("Delivery failed");
        failed.setChannel(NotificationChannel.IN_APP);
        failed.setStatus(NotificationStatus.FAILED);
        failed.setScheduledAt(reminder.getNextTriggerAt() == null ? now : reminder.getNextTriggerAt());
        failed.setRetryCount((int) previousFailures + 1);
        failed.setReminderId(reminder.getId());
        failed.setSourceType(reminder.getSourceType());
        failed.setSourceResourceId(reminder.getSourceResourceId());
        failed.setDeleted(false);
        notificationRepository.save(failed);
        log.warn("Recorded FAILED notification for reminder id={}", reminderId);
    }

    private ReminderExecutionService delegate() {
        return self != null ? self : this;
    }
}
