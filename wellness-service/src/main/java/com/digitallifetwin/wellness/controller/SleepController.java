package com.digitallifetwin.wellness.controller;

import com.digitallifetwin.wellness.dto.request.CreateSleepRequest;
import com.digitallifetwin.wellness.dto.request.UpdateSleepRequest;
import com.digitallifetwin.wellness.dto.response.PageResponse;
import com.digitallifetwin.wellness.dto.response.SleepResponse;
import com.digitallifetwin.wellness.security.SecurityUtils;
import com.digitallifetwin.wellness.service.SleepService;
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
@RequestMapping("/api/v1/wellness/sleep")
@RequiredArgsConstructor
@Tag(name = "Sleep")
public class SleepController {

    private final SleepService sleepService;

    @PostMapping
    @Operation(summary = "Create a sleep record")
    public ResponseEntity<SleepResponse> create(@Valid @RequestBody CreateSleepRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sleepService.create(SecurityUtils.currentUserId(), request));
    }

    @GetMapping
    @Operation(summary = "List sleep records")
    public ResponseEntity<PageResponse<SleepResponse>> list(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort as property,direction")
            @RequestParam(defaultValue = "wakeTime,desc") String sort) {
        return ResponseEntity.ok(sleepService.list(
                SecurityUtils.currentUserId(), from, to, Pageables.of(page, size, sort, "wakeTime")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a sleep record by id")
    public ResponseEntity<SleepResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(sleepService.getById(SecurityUtils.currentUserId(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a sleep record")
    public ResponseEntity<SleepResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateSleepRequest request) {
        return ResponseEntity.ok(sleepService.update(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a sleep record")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        sleepService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
