package com.digitallifetwin.wellness.controller;

import com.digitallifetwin.wellness.dto.request.CreateWaterRequest;
import com.digitallifetwin.wellness.dto.request.UpdateWaterRequest;
import com.digitallifetwin.wellness.dto.response.PageResponse;
import com.digitallifetwin.wellness.dto.response.WaterResponse;
import com.digitallifetwin.wellness.enums.BeverageType;
import com.digitallifetwin.wellness.security.SecurityUtils;
import com.digitallifetwin.wellness.service.WaterService;
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
@RequestMapping("/api/v1/wellness/water")
@RequiredArgsConstructor
@Tag(name = "Water")
public class WaterController {

    private final WaterService waterService;

    @PostMapping
    @Operation(summary = "Create a water record")
    public ResponseEntity<WaterResponse> create(@Valid @RequestBody CreateWaterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(waterService.create(SecurityUtils.currentUserId(), request));
    }

    @GetMapping
    @Operation(summary = "List water records")
    public ResponseEntity<PageResponse<WaterResponse>> list(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) BeverageType beverageType,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort as property,direction")
            @RequestParam(defaultValue = "consumedAt,desc") String sort) {
        return ResponseEntity.ok(waterService.list(
                SecurityUtils.currentUserId(), from, to, beverageType,
                Pageables.of(page, size, sort, "consumedAt")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a water record by id")
    public ResponseEntity<WaterResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(waterService.getById(SecurityUtils.currentUserId(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a water record")
    public ResponseEntity<WaterResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateWaterRequest request) {
        return ResponseEntity.ok(waterService.update(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a water record")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        waterService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
