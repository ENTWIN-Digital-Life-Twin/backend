package com.digitallifetwin.wellness.repository;

import com.digitallifetwin.wellness.entity.Meal;
import com.digitallifetwin.wellness.enums.MealType;
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

public interface MealRepository extends JpaRepository<Meal, UUID>, JpaSpecificationExecutor<Meal> {

    Optional<Meal> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    Page<Meal> findByUserIdAndDeletedFalseAndMealTimeBetween(
            UUID userId, Instant from, Instant to, Pageable pageable);

    List<Meal> findByUserIdAndDeletedFalseAndMealTimeGreaterThanEqualAndMealTimeLessThan(
            UUID userId, Instant start, Instant end);

    static Specification<Meal> withFilters(UUID userId, Instant from, Instant to, MealType mealType) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.isFalse(root.get("deleted")));
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("mealTime"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("mealTime"), to));
            }
            if (mealType != null) {
                predicates.add(cb.equal(root.get("mealType"), mealType));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
