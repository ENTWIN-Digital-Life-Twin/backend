package com.digitallifetwin.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitallifetwin.notification.entity.Notification;
import com.digitallifetwin.notification.entity.Reminder;
import com.digitallifetwin.notification.enums.NotificationStatus;
import com.digitallifetwin.notification.enums.RecurrenceType;
import com.digitallifetwin.notification.enums.ReminderType;
import com.digitallifetwin.notification.repository.NotificationRepository;
import com.digitallifetwin.notification.repository.ReminderRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ReminderExecutionServiceTest {

    @Mock
    private ReminderRepository reminderRepository;
    @Mock
    private NotificationRepository notificationRepository;

    private ReminderExecutionService reminderExecutionService;

    @BeforeEach
    void setUp() {
        reminderExecutionService = new ReminderExecutionService(
                reminderRepository, notificationRepository, null);
    }

    @Test
    void processDue_oneTime_createsNotificationAndDisables() {
        UUID reminderId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-24T10:00:00Z");
        Reminder reminder = dueReminder(reminderId, now.minusSeconds(60), false, RecurrenceType.NONE);

        when(reminderRepository.findDueIds(eq(now), any(Pageable.class))).thenReturn(List.of(reminderId));
        when(reminderRepository.findByIdForUpdate(reminderId)).thenReturn(Optional.of(reminder));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int processed = reminderExecutionService.processDueReminders(now);

        assertThat(processed).isEqualTo(1);
        assertThat(reminder.isEnabled()).isFalse();
        assertThat(reminder.getLastTriggeredAt()).isEqualTo(now);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(captor.getValue().getChannel().name()).isEqualTo("IN_APP");
    }

    @Test
    void processDue_secondPass_doesNotDuplicateWhenNoLongerDue() {
        UUID reminderId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-24T10:00:00Z");
        Reminder reminder = dueReminder(reminderId, now.minusSeconds(60), false, RecurrenceType.NONE);
        reminder.setEnabled(false);

        when(reminderRepository.findDueIds(eq(now), any(Pageable.class))).thenReturn(List.of(reminderId));
        when(reminderRepository.findByIdForUpdate(reminderId)).thenReturn(Optional.of(reminder));

        int processed = reminderExecutionService.processDueReminders(now);

        assertThat(processed).isEqualTo(0);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void processDue_futureReminder_skipped() {
        UUID reminderId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-24T10:00:00Z");
        Reminder reminder = dueReminder(reminderId, now.plusSeconds(3600), false, RecurrenceType.NONE);

        when(reminderRepository.findDueIds(eq(now), any(Pageable.class))).thenReturn(List.of(reminderId));
        when(reminderRepository.findByIdForUpdate(reminderId)).thenReturn(Optional.of(reminder));

        assertThat(reminderExecutionService.processDueReminders(now)).isZero();
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void processDue_daily_staysEnabledAndAdvancesOneDay() {
        UUID reminderId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-24T10:00:00Z");
        Instant trigger = Instant.parse("2026-08-24T09:00:00Z");
        Reminder reminder = dueReminder(reminderId, trigger, true, RecurrenceType.DAILY);

        when(reminderRepository.findDueIds(eq(now), any(Pageable.class))).thenReturn(List.of(reminderId));
        when(reminderRepository.findByIdForUpdate(reminderId)).thenReturn(Optional.of(reminder));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reminderExecutionService.processDueReminders(now);

        assertThat(reminder.isEnabled()).isTrue();
        assertThat(reminder.getNextTriggerAt()).isEqualTo(Instant.parse("2026-08-25T09:00:00Z"));
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    private static Reminder dueReminder(
            UUID id, Instant nextTriggerAt, boolean recurring, RecurrenceType recurrenceType) {
        Reminder reminder = new Reminder();
        reminder.setId(id);
        reminder.setUserId(UUID.randomUUID());
        reminder.setTitle("Drink water");
        reminder.setMessage("Time to drink");
        reminder.setReminderType(ReminderType.WATER);
        reminder.setTriggerDateTime(nextTriggerAt);
        reminder.setRecurring(recurring);
        reminder.setRecurrenceType(recurrenceType);
        reminder.setEnabled(true);
        reminder.setDeleted(false);
        reminder.setAdvanceMinutes(0);
        reminder.setNextTriggerAt(nextTriggerAt);
        return reminder;
    }
}
