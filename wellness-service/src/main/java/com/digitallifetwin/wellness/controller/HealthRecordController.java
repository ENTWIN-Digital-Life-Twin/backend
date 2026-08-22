package com.digitallifetwin.wellness.controller;

import com.digitallifetwin.wellness.dto.request.CreateHealthRecordRequest;
import com.digitallifetwin.wellness.dto.request.UpdateHealthRecordRequest;
import com.digitallifetwin.wellness.dto.response.HealthRecordResponse;
import com.digitallifetwin.wellness.dto.response.PageResponse;
import com.digitallifetwin.wellness.security.SecurityUtils;
import com.digitallifetwin.wellness.service.HealthRecordService;
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
@RequestMapping("/api/v1/wellness/health-records")
@RequiredArgsConstructor
@Tag(name = "Health records")
public class HealthRecordController {

    private final HealthRecordService healthRecordService;

    @PostMapping
    @Operation(summary = "Create a health record")
    public ResponseEntity<HealthRecordResponse> create(@Valid @RequestBody CreateHealthRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(healthRecordService.create(SecurityUtils.currentUserId(), request));
    }

    @GetMapping
    @Operation(summary = "List health records")
    public ResponseEntity<PageResponse<HealthRecordResponse>> list(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort as property,direction")
            @RequestParam(defaultValue = "recordedAt,desc") String sort) {
        return ResponseEntity.ok(healthRecordService.list(
                SecurityUtils.currentUserId(), from, to, Pageables.of(page, size, sort, "recordedAt")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a health record by id")
    public ResponseEntity<HealthRecordResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(healthRecordService.getById(SecurityUtils.currentUserId(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a health record")
    public ResponseEntity<HealthRecordResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateHealthRecordRequest request) {
        return ResponseEntity.ok(healthRecordService.update(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a health record")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        healthRecordService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
