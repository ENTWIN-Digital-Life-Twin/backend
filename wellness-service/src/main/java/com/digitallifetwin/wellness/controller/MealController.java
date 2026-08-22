package com.digitallifetwin.wellness.controller;

import com.digitallifetwin.wellness.dto.request.CreateMealRequest;
import com.digitallifetwin.wellness.dto.request.UpdateMealRequest;
import com.digitallifetwin.wellness.dto.response.MealResponse;
import com.digitallifetwin.wellness.dto.response.PageResponse;
import com.digitallifetwin.wellness.enums.MealType;
import com.digitallifetwin.wellness.security.SecurityUtils;
import com.digitallifetwin.wellness.service.MealService;
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
@RequestMapping("/api/v1/wellness/meals")
@RequiredArgsConstructor
@Tag(name = "Meals")
public class MealController {

    private final MealService mealService;

    @PostMapping
    @Operation(summary = "Create a meal")
    public ResponseEntity<MealResponse> create(@Valid @RequestBody CreateMealRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mealService.create(SecurityUtils.currentUserId(), request));
    }

    @GetMapping
    @Operation(summary = "List meals")
    public ResponseEntity<PageResponse<MealResponse>> list(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) MealType mealType,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort as property,direction")
            @RequestParam(defaultValue = "mealTime,desc") String sort) {
        return ResponseEntity.ok(mealService.list(
                SecurityUtils.currentUserId(), from, to, mealType,
                Pageables.of(page, size, sort, "mealTime")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a meal by id")
    public ResponseEntity<MealResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(mealService.getById(SecurityUtils.currentUserId(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a meal")
    public ResponseEntity<MealResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateMealRequest request) {
        return ResponseEntity.ok(mealService.update(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a meal")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        mealService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
