package com.digitallifetwin.wellness.service;

import com.digitallifetwin.wellness.dto.request.CreateWellnessGoalRequest;
import com.digitallifetwin.wellness.dto.request.UpdateGoalStatusRequest;
import com.digitallifetwin.wellness.dto.request.UpdateWellnessGoalRequest;
import com.digitallifetwin.wellness.dto.response.PageResponse;
import com.digitallifetwin.wellness.dto.response.WellnessGoalResponse;
import com.digitallifetwin.wellness.entity.WellnessGoal;
import com.digitallifetwin.wellness.enums.GoalStatus;
import com.digitallifetwin.wellness.exception.WellnessGoalNotFoundException;
import com.digitallifetwin.wellness.mapper.WellnessMapper;
import com.digitallifetwin.wellness.repository.WellnessGoalRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WellnessGoalService {

    private final WellnessGoalRepository wellnessGoalRepository;
    private final WellnessMapper wellnessMapper;

    @Transactional
    public WellnessGoalResponse create(UUID userId, CreateWellnessGoalRequest request) {
        validateDates(request.startDate(), request.targetDate());

        WellnessGoal goal = new WellnessGoal();
        goal.setUserId(userId);
        goal.setGoalType(request.goalType());
        goal.setTargetValue(request.targetValue());
        goal.setCurrentValue(request.currentValue() != null ? request.currentValue() : 0.0);
        goal.setUnit(request.unit().trim());
        goal.setStartDate(request.startDate());
        goal.setTargetDate(request.targetDate());
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setDeleted(false);
        return wellnessMapper.toWellnessGoalResponse(wellnessGoalRepository.save(goal));
    }

    @Transactional(readOnly = true)
    public WellnessGoalResponse getById(UUID userId, UUID id) {
        return wellnessMapper.toWellnessGoalResponse(requireOwned(userId, id));
    }

    @Transactional(readOnly = true)
    public PageResponse<WellnessGoalResponse> list(UUID userId, GoalStatus status, Pageable pageable) {
        Page<WellnessGoal> page = wellnessGoalRepository.findAll(
                WellnessGoalRepository.withFilters(userId, status), pageable);
        List<WellnessGoalResponse> content = page.getContent().stream()
                .map(wellnessMapper::toWellnessGoalResponse)
                .toList();
        return new PageResponse<>(
                content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public WellnessGoalResponse update(UUID userId, UUID id, UpdateWellnessGoalRequest request) {
        validateDates(request.startDate(), request.targetDate());
        WellnessGoal goal = requireOwned(userId, id);
        goal.setGoalType(request.goalType());
        goal.setTargetValue(request.targetValue());
        goal.setCurrentValue(request.currentValue());
        goal.setUnit(request.unit().trim());
        goal.setStartDate(request.startDate());
        goal.setTargetDate(request.targetDate());
        return wellnessMapper.toWellnessGoalResponse(wellnessGoalRepository.save(goal));
    }

    @Transactional
    public WellnessGoalResponse updateStatus(UUID userId, UUID id, UpdateGoalStatusRequest request) {
        WellnessGoal goal = requireOwned(userId, id);
        GoalStatusTransitions.assertAllowed(goal.getStatus(), request.status());

        goal.setStatus(request.status());
        if (request.status() == GoalStatus.COMPLETED) {
            goal.setCompletedAt(Instant.now());
        } else if (goal.getCompletedAt() != null) {
            goal.setCompletedAt(null);
        }

        return wellnessMapper.toWellnessGoalResponse(wellnessGoalRepository.save(goal));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        WellnessGoal goal = requireOwned(userId, id);
        goal.softDelete();
        wellnessGoalRepository.save(goal);
    }

    private WellnessGoal requireOwned(UUID userId, UUID id) {
        return wellnessGoalRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(WellnessGoalNotFoundException::new);
    }

    static void validateDates(LocalDate startDate, LocalDate targetDate) {
        if (targetDate != null && targetDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Target date cannot be before start date");
        }
    }
}
