package com.digitallifetwin.wellness.controller;

import com.digitallifetwin.wellness.dto.request.CreateWellnessGoalRequest;
import com.digitallifetwin.wellness.dto.request.UpdateGoalStatusRequest;
import com.digitallifetwin.wellness.dto.request.UpdateWellnessGoalRequest;
import com.digitallifetwin.wellness.dto.response.PageResponse;
import com.digitallifetwin.wellness.dto.response.WellnessGoalResponse;
import com.digitallifetwin.wellness.enums.GoalStatus;
import com.digitallifetwin.wellness.security.SecurityUtils;
import com.digitallifetwin.wellness.service.WellnessGoalService;
import com.digitallifetwin.wellness.util.Pageables;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wellness/goals")
@RequiredArgsConstructor
@Tag(name = "Wellness goals")
public class WellnessGoalController {

    private final WellnessGoalService wellnessGoalService;

    @PostMapping
    @Operation(summary = "Create a wellness goal")
    public ResponseEntity<WellnessGoalResponse> create(@Valid @RequestBody CreateWellnessGoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(wellnessGoalService.create(SecurityUtils.currentUserId(), request));
    }

    @GetMapping
    @Operation(summary = "List wellness goals")
    public ResponseEntity<PageResponse<WellnessGoalResponse>> list(
            @RequestParam(required = false) GoalStatus status,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort as property,direction")
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ResponseEntity.ok(wellnessGoalService.list(
                SecurityUtils.currentUserId(), status, Pageables.of(page, size, sort, "createdAt")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a wellness goal by id")
    public ResponseEntity<WellnessGoalResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(wellnessGoalService.getById(SecurityUtils.currentUserId(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a wellness goal")
    public ResponseEntity<WellnessGoalResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateWellnessGoalRequest request) {
        return ResponseEntity.ok(wellnessGoalService.update(SecurityUtils.currentUserId(), id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Transition wellness goal status")
    public ResponseEntity<WellnessGoalResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateGoalStatusRequest request) {
        return ResponseEntity.ok(wellnessGoalService.updateStatus(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a wellness goal")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        wellnessGoalService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
