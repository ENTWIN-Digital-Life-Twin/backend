package com.digitallifetwin.wellness.repository;

import com.digitallifetwin.wellness.entity.Workout;
import com.digitallifetwin.wellness.enums.ActivityType;
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

public interface WorkoutRepository extends JpaRepository<Workout, UUID>, JpaSpecificationExecutor<Workout> {

    Optional<Workout> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    Page<Workout> findByUserIdAndDeletedFalseAndStartedAtBetween(
            UUID userId, Instant from, Instant to, Pageable pageable);

    List<Workout> findByUserIdAndDeletedFalseAndStartedAtGreaterThanEqualAndStartedAtLessThan(
            UUID userId, Instant start, Instant end);

    static Specification<Workout> withFilters(UUID userId, Instant from, Instant to, ActivityType activityType) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.isFalse(root.get("deleted")));
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startedAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startedAt"), to));
            }
            if (activityType != null) {
                predicates.add(cb.equal(root.get("activityType"), activityType));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
