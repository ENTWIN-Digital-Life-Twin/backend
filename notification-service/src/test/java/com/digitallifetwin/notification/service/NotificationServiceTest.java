package com.digitallifetwin.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.digitallifetwin.notification.dto.response.NotificationResponse;
import com.digitallifetwin.notification.entity.Notification;
import com.digitallifetwin.notification.enums.NotificationChannel;
import com.digitallifetwin.notification.enums.NotificationStatus;
import com.digitallifetwin.notification.enums.NotificationType;
import com.digitallifetwin.notification.mapper.NotificationMapper;
import com.digitallifetwin.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Spy
    private NotificationMapper notificationMapper = new NotificationMapper();
    @InjectMocks
    private NotificationService notificationService;

    private UUID userId;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
    }

    @Test
    void markRead_unread_setsStatusAndReadAt() {
        Notification notification = unread();
        when(notificationRepository.findByIdAndUserIdAndDeletedFalse(notificationId, userId))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.markRead(userId, notificationId);

        assertThat(response.status()).isEqualTo(NotificationStatus.READ);
        assertThat(response.readAt()).isNotNull();
    }

    @Test
    void markRead_alreadyRead_isIdempotent() {
        Instant readAt = Instant.parse("2026-08-24T11:00:00Z");
        Notification notification = unread();
        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(readAt);
        when(notificationRepository.findByIdAndUserIdAndDeletedFalse(notificationId, userId))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.markRead(userId, notificationId);

        assertThat(response.status()).isEqualTo(NotificationStatus.READ);
        assertThat(response.readAt()).isEqualTo(readAt);
    }

    private Notification unread() {
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setUserId(userId);
        notification.setNotificationType(NotificationType.REMINDER);
        notification.setTitle("Drink water");
        notification.setMessage("Time to drink");
        notification.setChannel(NotificationChannel.IN_APP);
        notification.setStatus(NotificationStatus.SENT);
        notification.setScheduledAt(Instant.parse("2026-08-24T10:00:00Z"));
        notification.setSentAt(Instant.parse("2026-08-24T10:00:00Z"));
        notification.setRetryCount(0);
        notification.setDeleted(false);
        notification.setCreatedAt(Instant.now());
        notification.setUpdatedAt(Instant.now());
        return notification;
    }
}
