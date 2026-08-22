package com.digitallifetwin.planning.repository;

import com.digitallifetwin.planning.entity.TaskCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskCategoryRepository extends JpaRepository<TaskCategory, UUID> {

    @Query("""
            SELECT c FROM TaskCategory c
            WHERE c.active = true
              AND (c.systemCategory = true OR c.userId = :userId)
            ORDER BY c.systemCategory DESC, c.name ASC
            """)
    List<TaskCategory> findVisibleForUser(@Param("userId") UUID userId);

    Optional<TaskCategory> findByIdAndUserIdAndSystemCategoryFalse(UUID id, UUID userId);

    Optional<TaskCategory> findByIdAndActiveTrue(UUID id);

    boolean existsByUserIdAndNameIgnoreCaseAndSystemCategoryFalse(UUID userId, String name);

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM TaskCategory c
            WHERE c.id = :id
              AND c.active = true
              AND (c.systemCategory = true OR c.userId = :userId)
            """)
    boolean isAccessibleToUser(@Param("id") UUID id, @Param("userId") UUID userId);
}
