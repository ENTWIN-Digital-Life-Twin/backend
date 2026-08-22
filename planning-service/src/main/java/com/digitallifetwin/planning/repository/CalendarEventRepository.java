package com.digitallifetwin.planning.repository;

import com.digitallifetwin.planning.entity.CalendarEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CalendarEventRepository
        extends JpaRepository<CalendarEvent, UUID>, JpaSpecificationExecutor<CalendarEvent> {

    Optional<CalendarEvent> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    @Query("""
            SELECT e FROM CalendarEvent e
            WHERE e.userId = :userId
              AND e.deleted = false
              AND e.startDateTime < :rangeEnd
              AND e.endDateTime > :rangeStart
            """)
    List<CalendarEvent> findOverlappingRange(
            @Param("userId") UUID userId,
            @Param("rangeStart") Instant rangeStart,
            @Param("rangeEnd") Instant rangeEnd);

    static Specification<CalendarEvent> withFilters(UUID userId, Instant from, Instant to) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.isFalse(root.get("deleted")));
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endDateTime"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDateTime"), to));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
