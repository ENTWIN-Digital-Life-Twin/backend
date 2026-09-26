package com.digitallifetwin.notification.service;

import com.digitallifetwin.notification.dto.request.CreateNotificationRequest;
import com.digitallifetwin.notification.dto.response.BootstrapNotificationsResponse;
import com.digitallifetwin.notification.dto.response.NotificationResponse;
import com.digitallifetwin.notification.dto.response.PageResponse;
import com.digitallifetwin.notification.dto.response.UnreadCountResponse;
import com.digitallifetwin.notification.entity.Notification;
import com.digitallifetwin.notification.enums.NotificationChannel;
import com.digitallifetwin.notification.enums.NotificationStatus;
import com.digitallifetwin.notification.enums.NotificationType;
import com.digitallifetwin.notification.enums.ReminderSourceType;
import com.digitallifetwin.notification.exception.NotificationNotFoundException;
import com.digitallifetwin.notification.mapper.NotificationMapper;
import com.digitallifetwin.notification.repository.NotificationRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    @Transactional
    public NotificationResponse create(UUID userId, CreateNotificationRequest request) {
        return notificationMapper.toNotificationResponse(persist(
                userId,
                request.notificationType(),
                request.title().trim(),
                request.message().trim(),
                request.sourceType(),
                Instant.now()));
    }

    @Transactional
    public BootstrapNotificationsResponse bootstrap(UUID userId) {
        Instant now = Instant.now();
        int created = 0;
        for (Starter starter : STARTERS) {
            if (notificationRepository.existsByUserIdAndTitleAndDeletedFalse(userId, starter.title())) {
                continue;
            }
            persist(userId, starter.type(), starter.title(), starter.message(), starter.sourceType(),
                    now.minus(starter.hoursAgo(), ChronoUnit.HOURS));
            created++;
        }
        return new BootstrapNotificationsResponse(created);
    }

    private Notification persist(
            UUID userId,
            NotificationType type,
            String title,
            String message,
            ReminderSourceType sourceType,
            Instant scheduledAt) {
        Instant sentAt = Instant.now();
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setChannel(NotificationChannel.IN_APP);
        notification.setStatus(NotificationStatus.SENT);
        notification.setScheduledAt(scheduledAt);
        notification.setSentAt(sentAt);
        notification.setRetryCount(0);
        notification.setSourceType(sourceType);
        notification.setDeleted(false);
        return notificationRepository.save(notification);
    }

    private record Starter(
            NotificationType type,
            ReminderSourceType sourceType,
            String title,
            String message,
            int hoursAgo) {
    }

    private static final List<Starter> STARTERS = List.of(
            new Starter(NotificationType.SYSTEM, ReminderSourceType.CUSTOM,
                    "Bienvenue sur ENTWIN",
                    "Votre centre de notifications est prêt : rappels, alertes de sécurité et insights y apparaissent.",
                    1),
            new Starter(NotificationType.REMINDER, ReminderSourceType.WELLNESS_GOAL,
                    "Rappel hydratation",
                    "Pensez à boire un verre d'eau pour rester proche de votre objectif du jour.",
                    3),
            new Starter(NotificationType.REMINDER, ReminderSourceType.WELLNESS_GOAL,
                    "Rappel repas",
                    "Notez votre repas pour garder un suivi nutritionnel fiable.",
                    5),
            new Starter(NotificationType.REMINDER, ReminderSourceType.WELLNESS_GOAL,
                    "Pause active",
                    "Levez-vous quelques minutes : une courte marche aide à relancer la concentration.",
                    2),
            new Starter(NotificationType.REMINDER, ReminderSourceType.WELLNESS_GOAL,
                    "Préparer le sommeil",
                    "Diminuez les écrans et préparez-vous à une nuit plus calme.",
                    8),
            new Starter(NotificationType.REMINDER, ReminderSourceType.TASK,
                    "Tâche bientôt due",
                    "Une tâche approche de son échéance. Ouvrez Planning pour la traiter ou la reporter.",
                    4),
            new Starter(NotificationType.REMINDER, ReminderSourceType.EVENT,
                    "Événement dans 30 minutes",
                    "Un rendez-vous de votre calendrier commence bientôt.",
                    1),
            new Starter(NotificationType.INFO, ReminderSourceType.CUSTOM,
                    "Analyse du jour",
                    "Votre assistant peut vous proposer un plan pour le reste de la journée à partir de vos habitudes.",
                    6),
            new Starter(NotificationType.WARNING, ReminderSourceType.WELLNESS_GOAL,
                    "Objectif hydratation",
                    "Vous êtes encore un peu en dessous de votre objectif d'eau. Un dernier verre peut suffire.",
                    7)
    );

    private Notification requireOwned(UUID userId, UUID id) {
        return notificationRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(NotificationNotFoundException::new);
    }
}
