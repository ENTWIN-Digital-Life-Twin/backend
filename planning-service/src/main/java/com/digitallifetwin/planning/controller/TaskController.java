package com.digitallifetwin.planning.controller;

import com.digitallifetwin.planning.dto.request.CreateTaskRequest;
import com.digitallifetwin.planning.dto.request.UpdateTaskRequest;
import com.digitallifetwin.planning.dto.request.UpdateTaskStatusRequest;
import com.digitallifetwin.planning.dto.response.PageResponse;
import com.digitallifetwin.planning.dto.response.TaskResponse;
import com.digitallifetwin.planning.enums.TaskPriority;
import com.digitallifetwin.planning.enums.TaskStatus;
import com.digitallifetwin.planning.security.SecurityUtils;
import com.digitallifetwin.planning.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Create a task for the authenticated user")
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.create(SecurityUtils.currentUserId(), request));
    }

    @GetMapping
    @Operation(summary = "List tasks with pagination and optional filters")
    public ResponseEntity<PageResponse<TaskResponse>> list(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort as property,direction — e.g. createdAt,desc")
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Pageable pageable = toPageable(page, size, sort, "createdAt");
        return ResponseEntity.ok(taskService.list(
                SecurityUtils.currentUserId(), status, priority, categoryId, from, to, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a task by id")
    public ResponseEntity<TaskResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.getById(SecurityUtils.currentUserId(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task")
    public ResponseEntity<TaskResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.update(SecurityUtils.currentUserId(), id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Transition task status")
    public ResponseEntity<TaskResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateTaskStatusRequest request) {
        return ResponseEntity.ok(taskService.updateStatus(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a task")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        taskService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    private Pageable toPageable(int page, int size, String sort, String defaultProperty) {
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? 20 : Math.min(size, 100);
        String[] parts = sort == null ? new String[0] : sort.split(",");
        String property = parts.length > 0 && !parts[0].isBlank() ? parts[0].trim() : defaultProperty;
        // Guard against Swagger sending JSON-like values such as ["createdAt,desc"]
        property = property.replace("[", "").replace("]", "").replace("\"", "").trim();
        if (property.isBlank()) {
            property = defaultProperty;
        }
        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim().replace("\"", "").replace("]", ""))) {
            direction = Sort.Direction.ASC;
        }
        return PageRequest.of(safePage, safeSize, Sort.by(direction, property));
    }
}
