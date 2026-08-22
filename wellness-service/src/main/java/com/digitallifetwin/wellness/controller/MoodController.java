package com.digitallifetwin.wellness.controller;

import com.digitallifetwin.wellness.dto.request.CreateMoodRequest;
import com.digitallifetwin.wellness.dto.request.UpdateMoodRequest;
import com.digitallifetwin.wellness.dto.response.MoodResponse;
import com.digitallifetwin.wellness.dto.response.PageResponse;
import com.digitallifetwin.wellness.security.SecurityUtils;
import com.digitallifetwin.wellness.service.MoodService;
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
@RequestMapping("/api/v1/wellness/mood")
@RequiredArgsConstructor
@Tag(name = "Mood")
public class MoodController {

    private final MoodService moodService;

    @PostMapping
    @Operation(summary = "Create a mood record")
    public ResponseEntity<MoodResponse> create(@Valid @RequestBody CreateMoodRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moodService.create(SecurityUtils.currentUserId(), request));
    }

    @GetMapping
    @Operation(summary = "List mood records")
    public ResponseEntity<PageResponse<MoodResponse>> list(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort as property,direction")
            @RequestParam(defaultValue = "recordedAt,desc") String sort) {
        return ResponseEntity.ok(moodService.list(
                SecurityUtils.currentUserId(), from, to, Pageables.of(page, size, sort, "recordedAt")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a mood record by id")
    public ResponseEntity<MoodResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(moodService.getById(SecurityUtils.currentUserId(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a mood record")
    public ResponseEntity<MoodResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateMoodRequest request) {
        return ResponseEntity.ok(moodService.update(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a mood record")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        moodService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
