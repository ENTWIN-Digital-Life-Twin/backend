package com.digitallifetwin.wellness.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.digitallifetwin.wellness.dto.request.CreateWaterRequest;
import com.digitallifetwin.wellness.entity.WaterRecord;
import com.digitallifetwin.wellness.enums.BeverageType;
import com.digitallifetwin.wellness.mapper.WellnessMapper;
import com.digitallifetwin.wellness.repository.WaterRecordRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WaterServiceTest {

    @Mock
    private WaterRecordRepository waterRecordRepository;
    @Spy
    private WellnessMapper wellnessMapper = new WellnessMapper();
    @InjectMocks
    private WaterService waterService;

    @Test
    void create_persistsRecord() {
        UUID userId = UUID.randomUUID();
        when(waterRecordRepository.save(any(WaterRecord.class))).thenAnswer(invocation -> {
            WaterRecord record = invocation.getArgument(0);
            record.setId(UUID.randomUUID());
            record.setCreatedAt(Instant.now());
            record.setUpdatedAt(Instant.now());
            return record;
        });

        var response = waterService.create(userId, new CreateWaterRequest(
                300, Instant.parse("2026-08-22T10:00:00Z"), BeverageType.WATER, null));

        org.assertj.core.api.Assertions.assertThat(response.quantityMl()).isEqualTo(300);
        org.assertj.core.api.Assertions.assertThat(response.beverageType()).isEqualTo(BeverageType.WATER);
    }
}
