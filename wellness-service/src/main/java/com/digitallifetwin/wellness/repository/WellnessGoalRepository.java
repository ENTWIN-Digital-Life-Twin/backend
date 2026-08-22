package com.digitallifetwin.wellness.repository;

import com.digitallifetwin.wellness.entity.WellnessGoal;
import com.digitallifetwin.wellness.enums.GoalStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WellnessGoalRepository extends JpaRepository<WellnessGoal, UUID>, JpaSpecificationExecutor<WellnessGoal> {

    Optional<WellnessGoal> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    static Specification<WellnessGoal> withFilters(UUID userId, GoalStatus status) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.isFalse(root.get("deleted")));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
