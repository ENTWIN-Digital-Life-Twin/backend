package com.digitallifetwin.wellness.controller;

import com.digitallifetwin.wellness.dto.request.CreateWorkoutRequest;
import com.digitallifetwin.wellness.dto.request.UpdateWorkoutRequest;
import com.digitallifetwin.wellness.dto.response.PageResponse;
import com.digitallifetwin.wellness.dto.response.WorkoutResponse;
import com.digitallifetwin.wellness.enums.ActivityType;
import com.digitallifetwin.wellness.security.SecurityUtils;
import com.digitallifetwin.wellness.service.WorkoutService;
import com.digitallifetwin.wellness.util.Pageables;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wellness/workouts")
@RequiredArgsConstructor
@Tag(name = "Workouts")
public class WorkoutController {

    private final WorkoutService workoutService;

    @PostMapping
    @Operation(summary = "Create a workout")
    public ResponseEntity<WorkoutResponse> create(@Valid @RequestBody CreateWorkoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workoutService.create(SecurityUtils.currentUserId(), request));
    }

    @GetMapping
    @Operation(summary = "List workouts")
    public ResponseEntity<PageResponse<WorkoutResponse>> list(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) ActivityType activityType,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort as property,direction")
            @RequestParam(defaultValue = "startedAt,desc") String sort) {
        return ResponseEntity.ok(workoutService.list(
                SecurityUtils.currentUserId(), from, to, activityType,
                Pageables.of(page, size, sort, "startedAt")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a workout by id")
    public ResponseEntity<WorkoutResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(workoutService.getById(SecurityUtils.currentUserId(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a workout")
    public ResponseEntity<WorkoutResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateWorkoutRequest request) {
        return ResponseEntity.ok(workoutService.update(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a workout")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        workoutService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
