package com.digitallifetwin.planning.controller;

import com.digitallifetwin.planning.dto.request.CreateEventRequest;
import com.digitallifetwin.planning.dto.request.UpdateEventRequest;
import com.digitallifetwin.planning.dto.response.EventResponse;
import com.digitallifetwin.planning.dto.response.PageResponse;
import com.digitallifetwin.planning.security.SecurityUtils;
import com.digitallifetwin.planning.service.EventService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Calendar Events")
public class EventController {

    private final EventService eventService;

    @PostMapping
    @Operation(summary = "Create a calendar event")
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.create(SecurityUtils.currentUserId(), request));
    }

    @GetMapping
    @Operation(summary = "List calendar events with optional date-range filters")
    public ResponseEntity<PageResponse<EventResponse>> list(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort as property,direction — e.g. startDateTime,asc")
            @RequestParam(defaultValue = "startDateTime,asc") String sort) {
        Pageable pageable = toPageable(page, size, sort, "startDateTime");
        return ResponseEntity.ok(eventService.list(SecurityUtils.currentUserId(), from, to, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a calendar event by id")
    public ResponseEntity<EventResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getById(SecurityUtils.currentUserId(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a calendar event")
    public ResponseEntity<EventResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateEventRequest request) {
        return ResponseEntity.ok(eventService.update(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a calendar event")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        eventService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    private Pageable toPageable(int page, int size, String sort, String defaultProperty) {
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? 20 : Math.min(size, 100);
        String[] parts = sort == null ? new String[0] : sort.split(",");
        String property = parts.length > 0 && !parts[0].isBlank() ? parts[0].trim() : defaultProperty;
        property = property.replace("[", "").replace("]", "").replace("\"", "").trim();
        if (property.isBlank()) {
            property = defaultProperty;
        }
        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim().replace("\"", "").replace("]", ""))) {
            direction = Sort.Direction.DESC;
        }
        return PageRequest.of(safePage, safeSize, Sort.by(direction, property));
    }
}
