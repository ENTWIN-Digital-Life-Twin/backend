package com.digitallifetwin.wellness.service;

import com.digitallifetwin.wellness.dto.request.CreateWorkoutRequest;
import com.digitallifetwin.wellness.dto.request.UpdateWorkoutRequest;
import com.digitallifetwin.wellness.dto.response.PageResponse;
import com.digitallifetwin.wellness.dto.response.WorkoutResponse;
import com.digitallifetwin.wellness.entity.Workout;
import com.digitallifetwin.wellness.enums.ActivityType;
import com.digitallifetwin.wellness.exception.WorkoutNotFoundException;
import com.digitallifetwin.wellness.mapper.WellnessMapper;
import com.digitallifetwin.wellness.repository.WorkoutRepository;
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
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final WellnessMapper wellnessMapper;

    @Transactional
    public WorkoutResponse create(UUID userId, CreateWorkoutRequest request) {
        validateDuration(request.durationMinutes());

        Workout workout = new Workout();
        workout.setUserId(userId);
        workout.setActivityType(request.activityType());
        workout.setStartedAt(request.startedAt());
        workout.setDurationMinutes(request.durationMinutes());
        workout.setIntensity(request.intensity());
        workout.setCaloriesBurned(request.caloriesBurned());
        workout.setAverageHeartRate(request.averageHeartRate());
        workout.setDistanceKm(request.distanceKm());
        workout.setCompleted(request.completed() == null || request.completed());
        workout.setNotes(trimToNull(request.notes()));
        workout.setDeleted(false);
        return wellnessMapper.toWorkoutResponse(workoutRepository.save(workout));
    }

    @Transactional(readOnly = true)
    public WorkoutResponse getById(UUID userId, UUID id) {
        return wellnessMapper.toWorkoutResponse(requireOwned(userId, id));
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkoutResponse> list(
            UUID userId, Instant from, Instant to, ActivityType activityType, Pageable pageable) {
        Page<Workout> page = workoutRepository.findAll(
                WorkoutRepository.withFilters(userId, from, to, activityType), pageable);
        List<WorkoutResponse> content = page.getContent().stream()
                .map(wellnessMapper::toWorkoutResponse)
                .toList();
        return new PageResponse<>(
                content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public WorkoutResponse update(UUID userId, UUID id, UpdateWorkoutRequest request) {
        validateDuration(request.durationMinutes());
        Workout workout = requireOwned(userId, id);
        workout.setActivityType(request.activityType());
        workout.setStartedAt(request.startedAt());
        workout.setDurationMinutes(request.durationMinutes());
        workout.setIntensity(request.intensity());
        workout.setCaloriesBurned(request.caloriesBurned());
        workout.setAverageHeartRate(request.averageHeartRate());
        workout.setDistanceKm(request.distanceKm());
        workout.setCompleted(request.completed() == null || request.completed());
        workout.setNotes(trimToNull(request.notes()));
        return wellnessMapper.toWorkoutResponse(workoutRepository.save(workout));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Workout workout = requireOwned(userId, id);
        workout.softDelete();
        workoutRepository.save(workout);
    }

    private Workout requireOwned(UUID userId, UUID id) {
        return workoutRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(WorkoutNotFoundException::new);
    }

    static void validateDuration(Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes < 1) {
            throw new IllegalArgumentException("Workout duration must be at least 1 minute");
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
