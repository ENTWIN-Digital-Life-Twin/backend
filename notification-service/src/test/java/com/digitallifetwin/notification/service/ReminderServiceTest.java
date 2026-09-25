package com.digitallifetwin.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitallifetwin.notification.dto.request.CreateReminderRequest;
import com.digitallifetwin.notification.dto.response.ReminderResponse;
import com.digitallifetwin.notification.entity.Reminder;
import com.digitallifetwin.notification.enums.RecurrenceType;
import com.digitallifetwin.notification.enums.ReminderSourceType;
import com.digitallifetwin.notification.enums.ReminderType;
import com.digitallifetwin.notification.exception.InvalidReminderConfigurationException;
import com.digitallifetwin.notification.mapper.NotificationMapper;
import com.digitallifetwin.notification.repository.ReminderRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private ReminderRepository reminderRepository;
    @Spy
    private NotificationMapper notificationMapper = new NotificationMapper();
    @InjectMocks
    private ReminderService reminderService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void create_oneTimeReminder_setsNextTriggerAt() {
        Instant trigger = Instant.parse("2026-08-24T10:00:00Z");
        CreateReminderRequest request = new CreateReminderRequest(
                "Drink water",
                "Time to drink some water",
                ReminderType.WATER,
                trigger,
                false,
                null,
                true,
                ReminderSourceType.CUSTOM,
                null,
                0
        );
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> {
            Reminder reminder = invocation.getArgument(0);
            reminder.setId(UUID.randomUUID());
            reminder.setCreatedAt(Instant.now());
            reminder.setUpdatedAt(Instant.now());
            return reminder;
        });

        ReminderResponse response = reminderService.create(userId, request);

        assertThat(response.enabled()).isTrue();
        assertThat(response.recurrenceType()).isEqualTo(RecurrenceType.NONE);
        assertThat(response.nextTriggerAt()).isEqualTo(trigger);
        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
    }

    @Test
    void create_recurringWithoutType_rejected() {
        CreateReminderRequest request = new CreateReminderRequest(
                "Daily water",
                null,
                ReminderType.WATER,
                Instant.parse("2026-08-24T10:00:00Z"),
                true,
                null,
                true,
                ReminderSourceType.CUSTOM,
                null,
                0
        );
        assertThatThrownBy(() -> reminderService.create(userId, request))
                .isInstanceOf(InvalidReminderConfigurationException.class);
    }
}
