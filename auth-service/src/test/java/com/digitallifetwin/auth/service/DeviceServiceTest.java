package com.digitallifetwin.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitallifetwin.auth.entity.UserDevice;
import com.digitallifetwin.auth.repository.UserDeviceRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @InjectMocks
    private DeviceService deviceService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void remember_blankDevice_isIgnored() {
        assertThat(deviceService.remember(userId, "  ", "Chrome")).isFalse();
        verify(userDeviceRepository, never()).save(any());
    }

    @Test
    void remember_firstDevice_isNotFlaggedAsNew() {
        when(userDeviceRepository.findByUserIdAndDeviceId(userId, "browser-a")).thenReturn(Optional.empty());
        when(userDeviceRepository.countByUserId(userId)).thenReturn(0L);
        when(userDeviceRepository.save(any(UserDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(deviceService.remember(userId, "browser-a", "Chrome on Windows")).isFalse();

        ArgumentCaptor<UserDevice> captor = ArgumentCaptor.forClass(UserDevice.class);
        verify(userDeviceRepository).save(captor.capture());
        assertThat(captor.getValue().getDeviceId()).isEqualTo("browser-a");
        assertThat(captor.getValue().getDeviceLabel()).isEqualTo("Chrome on Windows");
    }

    @Test
    void remember_knownDevice_updatesLastSeenWithoutFlag() {
        UserDevice existing = new UserDevice();
        existing.setUserId(userId);
        existing.setDeviceId("browser-a");
        when(userDeviceRepository.findByUserIdAndDeviceId(userId, "browser-a")).thenReturn(Optional.of(existing));
        when(userDeviceRepository.save(existing)).thenReturn(existing);

        assertThat(deviceService.remember(userId, "BROWSER-A", "Firefox")).isFalse();
        assertThat(existing.getDeviceLabel()).isEqualTo("Firefox");
    }

    @Test
    void remember_secondDevice_isFlaggedAsNew() {
        when(userDeviceRepository.findByUserIdAndDeviceId(userId, "browser-b")).thenReturn(Optional.empty());
        when(userDeviceRepository.countByUserId(userId)).thenReturn(1L);
        when(userDeviceRepository.save(any(UserDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(deviceService.remember(userId, "browser-b", "Safari")).isTrue();
    }
}
