package com.digitallifetwin.auth.repository;

import com.digitallifetwin.auth.entity.UserPreference;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {

    Optional<UserPreference> findByUser_Id(UUID userId);
}
