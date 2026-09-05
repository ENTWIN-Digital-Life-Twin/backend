package com.digitallifetwin.notification.service;

import com.digitallifetwin.notification.dto.response.NotificationResponse;
import com.digitallifetwin.notification.dto.response.PageResponse;
import com.digitallifetwin.notification.dto.response.UnreadCountResponse;
import com.digitallifetwin.notification.entity.Notification;
import com.digitallifetwin.notification.enums.NotificationStatus;
import com.digitallifetwin.notification.enums.NotificationType;
import com.digitallifetwin.notification.exception.NotificationNotFoundException;
import com.digitallifetwin.notification.mapper.NotificationMapper;
import com.digitallifetwin.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Transactional(readOnly = true)
    public NotificationResponse getById(UUID userId, UUID id) {
        return notificationMapper.toNotificationResponse(requireOwned(userId, id));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(
            UUID userId,
            NotificationStatus status,
            NotificationType notificationType,
            Instant from,
            Instant to,
            Boolean unreadOnly,
            Pageable pageable) {
        Page<Notification> page = notificationRepository.findAll(
                NotificationRepository.withFilters(userId, status, notificationType, from, to, unreadOnly),
                pageable);
        List<NotificationResponse> content = page.getContent().stream()
                .map(notificationMapper::toNotificationResponse)
                .toList();
        return new PageResponse<>(
                content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID id) {
        Notification notification = requireOwned(userId, id);
        if (notification.getReadAt() == null) {
            Instant now = Instant.now();
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(now);
        }
        return notificationMapper.toNotificationResponse(notificationRepository.save(notification));
    }

    @Transactional
    public UnreadCountResponse markAllRead(UUID userId) {
        Instant now = Instant.now();
        notificationRepository.markAllUnreadAsRead(userId, now);
        return unreadCount(userId);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(UUID userId) {
        return new UnreadCountResponse(notificationRepository.countByUserIdAndDeletedFalseAndReadAtIsNull(userId));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Notification notification = requireOwned(userId, id);
        notification.softDelete();
        notificationRepository.save(notification);
    }

    private Notification requireOwned(UUID userId, UUID id) {
        return notificationRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(NotificationNotFoundException::new);
    }
}
