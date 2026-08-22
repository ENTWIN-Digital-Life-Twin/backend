package com.digitallifetwin.wellness.service;

import com.digitallifetwin.wellness.dto.request.CreateWaterRequest;
import com.digitallifetwin.wellness.dto.request.UpdateWaterRequest;
import com.digitallifetwin.wellness.dto.response.PageResponse;
import com.digitallifetwin.wellness.dto.response.WaterResponse;
import com.digitallifetwin.wellness.entity.WaterRecord;
import com.digitallifetwin.wellness.enums.BeverageType;
import com.digitallifetwin.wellness.exception.WaterRecordNotFoundException;
import com.digitallifetwin.wellness.mapper.WellnessMapper;
import com.digitallifetwin.wellness.repository.WaterRecordRepository;
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
public class WaterService {

    private final WaterRecordRepository waterRecordRepository;
    private final WellnessMapper wellnessMapper;

    @Transactional
    public WaterResponse create(UUID userId, CreateWaterRequest request) {
        WaterRecord record = new WaterRecord();
        record.setUserId(userId);
        record.setQuantityMl(request.quantityMl());
        record.setConsumedAt(request.consumedAt());
        record.setBeverageType(request.beverageType());
        record.setNotes(trimToNull(request.notes()));
        record.setDeleted(false);
        return wellnessMapper.toWaterResponse(waterRecordRepository.save(record));
    }

    @Transactional(readOnly = true)
    public WaterResponse getById(UUID userId, UUID id) {
        return wellnessMapper.toWaterResponse(requireOwned(userId, id));
    }

    @Transactional(readOnly = true)
    public PageResponse<WaterResponse> list(
            UUID userId, Instant from, Instant to, BeverageType beverageType, Pageable pageable) {
        Page<WaterRecord> page = waterRecordRepository.findAll(
                WaterRecordRepository.withFilters(userId, from, to, beverageType), pageable);
        List<WaterResponse> content = page.getContent().stream()
                .map(wellnessMapper::toWaterResponse)
                .toList();
        return new PageResponse<>(
                content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public WaterResponse update(UUID userId, UUID id, UpdateWaterRequest request) {
        WaterRecord record = requireOwned(userId, id);
        record.setQuantityMl(request.quantityMl());
        record.setConsumedAt(request.consumedAt());
        record.setBeverageType(request.beverageType());
        record.setNotes(trimToNull(request.notes()));
        return wellnessMapper.toWaterResponse(waterRecordRepository.save(record));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        WaterRecord record = requireOwned(userId, id);
        record.softDelete();
        waterRecordRepository.save(record);
    }

    private WaterRecord requireOwned(UUID userId, UUID id) {
        return waterRecordRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(WaterRecordNotFoundException::new);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
