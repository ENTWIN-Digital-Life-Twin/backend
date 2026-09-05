package com.digitallifetwin.notification.repository;

import com.digitallifetwin.notification.entity.Reminder;
import com.digitallifetwin.notification.enums.ReminderType;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.QueryHint;

public interface ReminderRepository extends JpaRepository<Reminder, UUID>, JpaSpecificationExecutor<Reminder> {

    Optional<Reminder> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("SELECT r FROM Reminder r WHERE r.id = :id")
    Optional<Reminder> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            SELECT r.id FROM Reminder r
            WHERE r.enabled = true
              AND r.deleted = false
              AND r.nextTriggerAt <= :now
            ORDER BY r.nextTriggerAt ASC
            """)
    List<UUID> findDueIds(@Param("now") Instant now, Pageable pageable);

    static Specification<Reminder> withFilters(
            UUID userId,
            Boolean enabled,
            ReminderType reminderType,
            Instant from,
            Instant to) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.isFalse(root.get("deleted")));
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }
            if (reminderType != null) {
                predicates.add(cb.equal(root.get("reminderType"), reminderType));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("triggerDateTime"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("triggerDateTime"), to));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
