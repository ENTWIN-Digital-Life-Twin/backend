package com.digitallifetwin.notification.repository;

import com.digitallifetwin.notification.entity.Notification;
import com.digitallifetwin.notification.enums.NotificationStatus;
import com.digitallifetwin.notification.enums.NotificationType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID>, JpaSpecificationExecutor<Notification> {

    Optional<Notification> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    long countByUserIdAndDeletedFalseAndReadAtIsNull(UUID userId);

    long countByUserIdAndReminderIdAndDeletedFalse(UUID userId, UUID reminderId);

    boolean existsByUserIdAndTitleAndDeletedFalse(UUID userId, String title);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Notification n
            SET n.status = com.digitallifetwin.notification.enums.NotificationStatus.READ,
                n.readAt = :readAt,
                n.updatedAt = :readAt
            WHERE n.userId = :userId
              AND n.deleted = false
              AND n.readAt IS NULL
            """)
    int markAllUnreadAsRead(@Param("userId") UUID userId, @Param("readAt") Instant readAt);

    static Specification<Notification> withFilters(
            UUID userId,
            NotificationStatus status,
            NotificationType notificationType,
            Instant from,
            Instant to,
            Boolean unreadOnly) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.isFalse(root.get("deleted")));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (notificationType != null) {
                predicates.add(cb.equal(root.get("notificationType"), notificationType));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("scheduledAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("scheduledAt"), to));
            }
            if (Boolean.TRUE.equals(unreadOnly)) {
                predicates.add(cb.isNull(root.get("readAt")));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
