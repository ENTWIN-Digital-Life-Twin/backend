package com.digitallifetwin.wellness.service;

import com.digitallifetwin.wellness.dto.request.CreateMoodRequest;
import com.digitallifetwin.wellness.dto.request.UpdateMoodRequest;
import com.digitallifetwin.wellness.dto.response.MoodResponse;
import com.digitallifetwin.wellness.dto.response.PageResponse;
import com.digitallifetwin.wellness.entity.MoodRecord;
import com.digitallifetwin.wellness.exception.MoodRecordNotFoundException;
import com.digitallifetwin.wellness.mapper.WellnessMapper;
import com.digitallifetwin.wellness.repository.MoodRecordRepository;
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
public class MoodService {

    private final MoodRecordRepository moodRecordRepository;
    private final WellnessMapper wellnessMapper;

    @Transactional
    public MoodResponse create(UUID userId, CreateMoodRequest request) {
        validateLevels(request.moodLevel(), request.stressLevel(), request.fatigueLevel());

        MoodRecord record = new MoodRecord();
        record.setUserId(userId);
        record.setRecordedAt(request.recordedAt());
        record.setMoodLevel(request.moodLevel());
        record.setStressLevel(request.stressLevel());
        record.setFatigueLevel(request.fatigueLevel());
        record.setNotes(trimToNull(request.notes()));
        record.setDeleted(false);
        return wellnessMapper.toMoodResponse(moodRecordRepository.save(record));
    }

    @Transactional(readOnly = true)
    public MoodResponse getById(UUID userId, UUID id) {
        return wellnessMapper.toMoodResponse(requireOwned(userId, id));
    }

    @Transactional(readOnly = true)
    public PageResponse<MoodResponse> list(UUID userId, Instant from, Instant to, Pageable pageable) {
        Page<MoodRecord> page = moodRecordRepository.findAll(
                MoodRecordRepository.withFilters(userId, from, to), pageable);
        List<MoodResponse> content = page.getContent().stream()
                .map(wellnessMapper::toMoodResponse)
                .toList();
        return new PageResponse<>(
                content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public MoodResponse update(UUID userId, UUID id, UpdateMoodRequest request) {
        validateLevels(request.moodLevel(), request.stressLevel(), request.fatigueLevel());
        MoodRecord record = requireOwned(userId, id);
        record.setRecordedAt(request.recordedAt());
        record.setMoodLevel(request.moodLevel());
        record.setStressLevel(request.stressLevel());
        record.setFatigueLevel(request.fatigueLevel());
        record.setNotes(trimToNull(request.notes()));
        return wellnessMapper.toMoodResponse(moodRecordRepository.save(record));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        MoodRecord record = requireOwned(userId, id);
        record.softDelete();
        moodRecordRepository.save(record);
    }

    private MoodRecord requireOwned(UUID userId, UUID id) {
        return moodRecordRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(MoodRecordNotFoundException::new);
    }

    static void validateLevels(Integer mood, Integer stress, Integer fatigue) {
        assertInRange("moodLevel", mood);
        assertInRange("stressLevel", stress);
        assertInRange("fatigueLevel", fatigue);
    }

    private static void assertInRange(String field, Integer value) {
        if (value == null || value < 1 || value > 10) {
            throw new IllegalArgumentException(field + " must be between 1 and 10");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
