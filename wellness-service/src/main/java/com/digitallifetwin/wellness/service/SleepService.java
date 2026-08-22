package com.digitallifetwin.wellness.service;

import com.digitallifetwin.wellness.dto.request.CreateSleepRequest;
import com.digitallifetwin.wellness.dto.request.UpdateSleepRequest;
import com.digitallifetwin.wellness.dto.response.PageResponse;
import com.digitallifetwin.wellness.dto.response.SleepResponse;
import com.digitallifetwin.wellness.entity.SleepRecord;
import com.digitallifetwin.wellness.exception.SleepRecordNotFoundException;
import com.digitallifetwin.wellness.mapper.WellnessMapper;
import com.digitallifetwin.wellness.repository.SleepRecordRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SleepService {

    private final SleepRecordRepository sleepRecordRepository;
    private final WellnessMapper wellnessMapper;

    @Transactional
    public SleepResponse create(UUID userId, CreateSleepRequest request) {
        int duration = computeDurationMinutes(request.sleepStart(), request.wakeTime());

        SleepRecord record = new SleepRecord();
        record.setUserId(userId);
        record.setSleepStart(request.sleepStart());
        record.setWakeTime(request.wakeTime());
        record.setDurationMinutes(duration);
        record.setQualityScore(request.qualityScore());
        record.setInterruptions(request.interruptions());
        record.setNotes(trimToNull(request.notes()));
        record.setDeleted(false);

        return wellnessMapper.toSleepResponse(sleepRecordRepository.save(record));
    }

    @Transactional(readOnly = true)
    public SleepResponse getById(UUID userId, UUID id) {
        return wellnessMapper.toSleepResponse(requireOwned(userId, id));
    }

    @Transactional(readOnly = true)
    public PageResponse<SleepResponse> list(UUID userId, Instant from, Instant to, Pageable pageable) {
        Page<SleepRecord> page = sleepRecordRepository.findAll(
                SleepRecordRepository.withFilters(userId, from, to), pageable);
        List<SleepResponse> content = page.getContent().stream()
                .map(wellnessMapper::toSleepResponse)
                .toList();
        return new PageResponse<>(
                content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public SleepResponse update(UUID userId, UUID id, UpdateSleepRequest request) {
        SleepRecord record = requireOwned(userId, id);
        int duration = computeDurationMinutes(request.sleepStart(), request.wakeTime());

        record.setSleepStart(request.sleepStart());
        record.setWakeTime(request.wakeTime());
        record.setDurationMinutes(duration);
        record.setQualityScore(request.qualityScore());
        record.setInterruptions(request.interruptions());
        record.setNotes(trimToNull(request.notes()));

        return wellnessMapper.toSleepResponse(sleepRecordRepository.save(record));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        SleepRecord record = requireOwned(userId, id);
        record.softDelete();
        sleepRecordRepository.save(record);
    }

    private SleepRecord requireOwned(UUID userId, UUID id) {
        return sleepRecordRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(SleepRecordNotFoundException::new);
    }

    static int computeDurationMinutes(Instant sleepStart, Instant wakeTime) {
        if (!wakeTime.isAfter(sleepStart)) {
            throw new IllegalArgumentException("Wake time must be after sleep start");
        }
        long minutes = ChronoUnit.MINUTES.between(sleepStart, wakeTime);
        if (minutes <= 0) {
            throw new IllegalArgumentException("Sleep duration must be positive");
        }
        return (int) minutes;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
