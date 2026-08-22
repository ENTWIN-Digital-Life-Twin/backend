package com.digitallifetwin.wellness.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitallifetwin.wellness.dto.request.CreateSleepRequest;
import com.digitallifetwin.wellness.dto.response.SleepResponse;
import com.digitallifetwin.wellness.entity.SleepRecord;
import com.digitallifetwin.wellness.exception.SleepRecordNotFoundException;
import com.digitallifetwin.wellness.mapper.WellnessMapper;
import com.digitallifetwin.wellness.repository.SleepRecordRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SleepServiceTest {

    @Mock
    private SleepRecordRepository sleepRecordRepository;
    @Spy
    private WellnessMapper wellnessMapper = new WellnessMapper();
    @InjectMocks
    private SleepService sleepService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void create_computesDurationFromTimes() {
        CreateSleepRequest request = new CreateSleepRequest(
                Instant.parse("2026-08-21T22:00:00Z"),
                Instant.parse("2026-08-22T06:00:00Z"),
                8,
                1,
                null
        );
        when(sleepRecordRepository.save(any(SleepRecord.class))).thenAnswer(invocation -> {
            SleepRecord record = invocation.getArgument(0);
            record.setId(UUID.randomUUID());
            record.setCreatedAt(Instant.now());
            record.setUpdatedAt(Instant.now());
            return record;
        });

        SleepResponse response = sleepService.create(userId, request);

        assertThat(response.durationMinutes()).isEqualTo(480);
        ArgumentCaptor<SleepRecord> captor = ArgumentCaptor.forClass(SleepRecord.class);
        verify(sleepRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getDurationMinutes()).isEqualTo(480);
    }

    @Test
    void create_wakeBeforeSleep_throws() {
        assertThatThrownBy(() -> sleepService.create(userId, new CreateSleepRequest(
                Instant.parse("2026-08-22T08:00:00Z"),
                Instant.parse("2026-08-22T06:00:00Z"),
                null,
                null,
                null
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wake time must be after");
    }

    @Test
    void getById_missing_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(sleepRecordRepository.findByIdAndUserIdAndDeletedFalse(id, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sleepService.getById(userId, id))
                .isInstanceOf(SleepRecordNotFoundException.class);
    }
}
