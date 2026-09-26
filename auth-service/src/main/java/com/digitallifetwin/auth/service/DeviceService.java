package com.digitallifetwin.auth.service;

import com.digitallifetwin.auth.entity.UserDevice;
import com.digitallifetwin.auth.repository.UserDeviceRepository;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final UserDeviceRepository userDeviceRepository;

    /**
     * Records a browser/device for the user.
     *
     * @return {@code true} when this is a <em>new</em> device and the user already had another one
     *         (first-ever device is not treated as suspicious).
     */
    @Transactional
    public boolean remember(UUID userId, String deviceId, String deviceLabel) {
        String normalizedId = normalizeId(deviceId);
        if (userId == null || normalizedId == null) {
            return false;
        }
        return userDeviceRepository.findByUserIdAndDeviceId(userId, normalizedId)
                .map(existing -> {
                    existing.setDeviceLabel(clipLabel(deviceLabel, existing.getDeviceLabel()));
                    userDeviceRepository.save(existing);
                    return false;
                })
                .orElseGet(() -> {
                    boolean hadOtherDevices = userDeviceRepository.countByUserId(userId) > 0;
                    UserDevice created = new UserDevice();
                    created.setUserId(userId);
                    created.setDeviceId(normalizedId);
                    created.setDeviceLabel(clipLabel(deviceLabel, null));
                    userDeviceRepository.save(created);
                    return hadOtherDevices;
                });
    }

    private static String normalizeId(String deviceId) {
        if (deviceId == null) {
            return null;
        }
        String trimmed = deviceId.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        String compact = trimmed.toLowerCase(Locale.ROOT);
        return compact.length() <= 64 ? compact : compact.substring(0, 64);
    }

    private static String clipLabel(String incoming, String fallback) {
        String value = incoming == null || incoming.isBlank() ? fallback : incoming.trim();
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= 255 ? value : value.substring(0, 255);
    }
}
