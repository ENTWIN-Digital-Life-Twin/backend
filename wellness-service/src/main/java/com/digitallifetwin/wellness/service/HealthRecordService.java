package com.digitallifetwin.wellness.service;

import com.digitallifetwin.wellness.dto.request.CreateHealthRecordRequest;
import com.digitallifetwin.wellness.dto.request.UpdateHealthRecordRequest;
import com.digitallifetwin.wellness.dto.response.HealthRecordResponse;
import com.digitallifetwin.wellness.dto.response.PageResponse;
import com.digitallifetwin.wellness.entity.HealthRecord;
import com.digitallifetwin.wellness.exception.HealthRecordNotFoundException;
import com.digitallifetwin.wellness.mapper.WellnessMapper;
import com.digitallifetwin.wellness.repository.HealthRecordRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HealthRecordService {

    private final HealthRecordRepository healthRecordRepository;
    private final WellnessMapper wellnessMapper;

    @Transactional
    public HealthRecordResponse create(UUID userId, CreateHealthRecordRequest request) {
        HealthRecord record = new HealthRecord();
        record.setUserId(userId);
        apply(record, request.recordedAt(), request.weightKg(), request.restingHeartRate(),
                request.systolicPressure(), request.diastolicPressure(), request.temperatureCelsius(),
                request.stepCount(), request.notes());
        record.setDeleted(false);
        return wellnessMapper.toHealthRecordResponse(healthRecordRepository.save(record));
    }

    @Transactional(readOnly = true)
    public HealthRecordResponse getById(UUID userId, UUID id) {
        return wellnessMapper.toHealthRecordResponse(requireOwned(userId, id));
    }

    @Transactional(readOnly = true)
    public PageResponse<HealthRecordResponse> list(UUID userId, Instant from, Instant to, Pageable pageable) {
        Page<HealthRecord> page = healthRecordRepository.findAll(
                HealthRecordRepository.withFilters(userId, from, to), pageable);
        List<HealthRecordResponse> content = page.getContent().stream()
                .map(wellnessMapper::toHealthRecordResponse)
                .toList();
        return new PageResponse<>(
                content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public HealthRecordResponse update(UUID userId, UUID id, UpdateHealthRecordRequest request) {
        HealthRecord record = requireOwned(userId, id);
        apply(record, request.recordedAt(), request.weightKg(), request.restingHeartRate(),
                request.systolicPressure(), request.diastolicPressure(), request.temperatureCelsius(),
                request.stepCount(), request.notes());
        return wellnessMapper.toHealthRecordResponse(healthRecordRepository.save(record));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        HealthRecord record = requireOwned(userId, id);
        record.softDelete();
        healthRecordRepository.save(record);
    }

    private HealthRecord requireOwned(UUID userId, UUID id) {
        return healthRecordRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(HealthRecordNotFoundException::new);
    }

    private void apply(
            HealthRecord record,
            Instant recordedAt,
            Double weightKg,
            Integer restingHeartRate,
            Integer systolicPressure,
            Integer diastolicPressure,
            Double temperatureCelsius,
            Integer stepCount,
            String notes) {
        record.setRecordedAt(recordedAt);
        record.setWeightKg(weightKg);
        record.setRestingHeartRate(restingHeartRate);
        record.setSystolicPressure(systolicPressure);
        record.setDiastolicPressure(diastolicPressure);
        record.setTemperatureCelsius(temperatureCelsius);
        record.setStepCount(stepCount);
        record.setNotes(trimToNull(notes));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
