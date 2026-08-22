package com.digitallifetwin.wellness.repository;

import com.digitallifetwin.wellness.entity.SleepRecord;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SleepRecordRepository extends JpaRepository<SleepRecord, UUID>, JpaSpecificationExecutor<SleepRecord> {

    Optional<SleepRecord> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    Page<SleepRecord> findByUserIdAndDeletedFalseAndWakeTimeBetween(
            UUID userId, Instant from, Instant to, Pageable pageable);

    List<SleepRecord> findByUserIdAndDeletedFalseAndWakeTimeGreaterThanEqualAndWakeTimeLessThan(
            UUID userId, Instant start, Instant end);

    static Specification<SleepRecord> withFilters(UUID userId, Instant from, Instant to) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.isFalse(root.get("deleted")));
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("wakeTime"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("wakeTime"), to));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
