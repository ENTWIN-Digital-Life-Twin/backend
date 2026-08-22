package com.digitallifetwin.planning.repository;

import com.digitallifetwin.planning.entity.Task;
import com.digitallifetwin.planning.enums.TaskPriority;
import com.digitallifetwin.planning.enums.TaskStatus;
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

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    Optional<Task> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    boolean existsByCategoryIdAndDeletedFalse(UUID categoryId);

    @Query("""
            SELECT t FROM Task t
            WHERE t.userId = :userId
              AND t.deleted = false
              AND t.status NOT IN (
                    com.digitallifetwin.planning.enums.TaskStatus.CANCELLED,
                    com.digitallifetwin.planning.enums.TaskStatus.COMPLETED
                  )
              AND t.startDateTime IS NOT NULL
              AND t.startDateTime < :rangeEnd
              AND t.startDateTime >= :lookbackStart
            """)
    List<Task> findScheduledCandidates(
            @Param("userId") UUID userId,
            @Param("lookbackStart") Instant lookbackStart,
            @Param("rangeEnd") Instant rangeEnd);

    @Query("""
            SELECT t FROM Task t
            WHERE t.userId = :userId
              AND t.deleted = false
              AND t.startDateTime IS NOT NULL
              AND t.startDateTime < :dayEnd
              AND t.startDateTime >= :lookbackStart
            """)
    List<Task> findDailyCandidates(
            @Param("userId") UUID userId,
            @Param("lookbackStart") Instant lookbackStart,
            @Param("dayEnd") Instant dayEnd);

    static Specification<Task> withFilters(
            UUID userId,
            TaskStatus status,
            TaskPriority priority,
            UUID categoryId,
            Instant from,
            Instant to) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.isFalse(root.get("deleted")));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (from != null) {
                predicates.add(cb.or(
                        cb.and(cb.isNotNull(root.get("startDateTime")), cb.greaterThanOrEqualTo(root.get("startDateTime"), from)),
                        cb.and(
                                cb.isNull(root.get("startDateTime")),
                                cb.isNotNull(root.get("deadline")),
                                cb.greaterThanOrEqualTo(root.get("deadline"), from)),
                        cb.and(
                                cb.isNull(root.get("startDateTime")),
                                cb.isNull(root.get("deadline")),
                                cb.greaterThanOrEqualTo(root.get("createdAt"), from))
                ));
            }
            if (to != null) {
                predicates.add(cb.or(
                        cb.and(cb.isNotNull(root.get("startDateTime")), cb.lessThanOrEqualTo(root.get("startDateTime"), to)),
                        cb.and(
                                cb.isNull(root.get("startDateTime")),
                                cb.isNotNull(root.get("deadline")),
                                cb.lessThanOrEqualTo(root.get("deadline"), to)),
                        cb.and(
                                cb.isNull(root.get("startDateTime")),
                                cb.isNull(root.get("deadline")),
                                cb.lessThanOrEqualTo(root.get("createdAt"), to))
                ));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
