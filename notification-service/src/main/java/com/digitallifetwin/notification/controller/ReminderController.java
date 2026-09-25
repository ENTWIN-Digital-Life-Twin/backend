package com.digitallifetwin.notification.controller;

import com.digitallifetwin.notification.dto.request.CreateReminderRequest;
import com.digitallifetwin.notification.dto.request.UpdateReminderEnabledRequest;
import com.digitallifetwin.notification.dto.request.UpdateReminderRequest;
import com.digitallifetwin.notification.dto.response.ErrorResponse;
import com.digitallifetwin.notification.dto.response.PageResponse;
import com.digitallifetwin.notification.dto.response.ReminderResponse;
import com.digitallifetwin.notification.enums.ReminderType;
import com.digitallifetwin.notification.security.SecurityUtils;
import com.digitallifetwin.notification.service.ReminderService;
import com.digitallifetwin.notification.util.Pageables;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Set;
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
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
@Tag(name = "Reminders")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Unauthenticated",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class ReminderController {

    private static final Set<String> SORT_PROPERTIES =
            Set.of("createdAt", "updatedAt", "triggerDateTime", "nextTriggerAt", "title");

    private final ReminderService reminderService;

    @PostMapping
    @Operation(summary = "Create a reminder for the authenticated user")
    @ApiResponse(responseCode = "201", description = "Created")
    public ResponseEntity<ReminderResponse> create(@Valid @RequestBody CreateReminderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reminderService.create(SecurityUtils.currentUserId(), request));
    }

    @GetMapping
    @Operation(summary = "List reminders with pagination and optional filters")
    public ResponseEntity<PageResponse<ReminderResponse>> list(
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) ReminderType reminderType,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort as property,direction — e.g. createdAt,desc")
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ResponseEntity.ok(reminderService.list(
                SecurityUtils.currentUserId(),
                enabled,
                reminderType,
                from,
                to,
                Pageables.of(page, size, sort, "createdAt", SORT_PROPERTIES)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a reminder by id")
    @ApiResponse(responseCode = "404", description = "Not found or wrong owner",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ReminderResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(reminderService.getById(SecurityUtils.currentUserId(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a reminder")
    public ResponseEntity<ReminderResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateReminderRequest request) {
        return ResponseEntity.ok(reminderService.update(SecurityUtils.currentUserId(), id, request));
    }

    @PatchMapping("/{id}/enabled")
    @Operation(summary = "Enable or disable a reminder")
    public ResponseEntity<ReminderResponse> updateEnabled(
            @PathVariable UUID id, @Valid @RequestBody UpdateReminderEnabledRequest request) {
        return ResponseEntity.ok(reminderService.updateEnabled(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a reminder")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        reminderService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
