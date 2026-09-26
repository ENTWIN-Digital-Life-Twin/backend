package com.digitallifetwin.auth.repository;

import com.digitallifetwin.auth.entity.UserDevice;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {

    Optional<UserDevice> findByUserIdAndDeviceId(UUID userId, String deviceId);

    long countByUserId(UUID userId);
}
