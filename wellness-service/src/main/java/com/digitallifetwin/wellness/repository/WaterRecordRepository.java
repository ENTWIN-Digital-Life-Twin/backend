package com.digitallifetwin.wellness.repository;

import com.digitallifetwin.wellness.entity.WaterRecord;
import com.digitallifetwin.wellness.enums.BeverageType;
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

public interface WaterRecordRepository extends JpaRepository<WaterRecord, UUID>, JpaSpecificationExecutor<WaterRecord> {

    Optional<WaterRecord> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    Page<WaterRecord> findByUserIdAndDeletedFalseAndConsumedAtBetween(
            UUID userId, Instant from, Instant to, Pageable pageable);

    List<WaterRecord> findByUserIdAndDeletedFalseAndConsumedAtGreaterThanEqualAndConsumedAtLessThan(
            UUID userId, Instant start, Instant end);

    static Specification<WaterRecord> withFilters(UUID userId, Instant from, Instant to, BeverageType beverageType) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.isFalse(root.get("deleted")));
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("consumedAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("consumedAt"), to));
            }
            if (beverageType != null) {
                predicates.add(cb.equal(root.get("beverageType"), beverageType));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
