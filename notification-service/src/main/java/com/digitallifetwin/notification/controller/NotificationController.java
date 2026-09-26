package com.digitallifetwin.notification.controller;

import com.digitallifetwin.notification.dto.request.CreateNotificationRequest;
import com.digitallifetwin.notification.dto.response.BootstrapNotificationsResponse;
import com.digitallifetwin.notification.dto.response.ErrorResponse;
import com.digitallifetwin.notification.dto.response.NotificationResponse;
import com.digitallifetwin.notification.dto.response.PageResponse;
import com.digitallifetwin.notification.dto.response.UnreadCountResponse;
import com.digitallifetwin.notification.enums.NotificationStatus;
import com.digitallifetwin.notification.enums.NotificationType;
import com.digitallifetwin.notification.security.SecurityUtils;
import com.digitallifetwin.notification.service.NotificationService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Unauthenticated",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class NotificationController {

    private static final Set<String> SORT_PROPERTIES =
            Set.of("createdAt", "updatedAt", "scheduledAt", "sentAt", "readAt");

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List in-app notifications for the authenticated user")
    public ResponseEntity<PageResponse<NotificationResponse>> list(
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) NotificationType notificationType,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Boolean unreadOnly,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort as property,direction — e.g. scheduledAt,desc")
            @RequestParam(defaultValue = "scheduledAt,desc") String sort) {
        return ResponseEntity.ok(notificationService.list(
                SecurityUtils.currentUserId(),
                status,
                notificationType,
                from,
                to,
                unreadOnly,
                Pageables.of(page, size, sort, "scheduledAt", SORT_PROPERTIES)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Unread notification count for the notification badge")
    public ResponseEntity<UnreadCountResponse> unreadCount() {
        return ResponseEntity.ok(notificationService.unreadCount(SecurityUtils.currentUserId()));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all unread notifications as read")
    public ResponseEntity<UnreadCountResponse> readAll() {
        return ResponseEntity.ok(notificationService.markAllRead(SecurityUtils.currentUserId()));
    }

    @PostMapping("/bootstrap")
    @Operation(summary = "Create starter reminder, wellness and system notifications if missing")
    public ResponseEntity<BootstrapNotificationsResponse> bootstrap() {
        return ResponseEntity.ok(notificationService.bootstrap(SecurityUtils.currentUserId()));
    }

    @PostMapping
    @Operation(summary = "Create an in-app notification for the authenticated user")
    public ResponseEntity<NotificationResponse> create(@Valid @RequestBody CreateNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.create(SecurityUtils.currentUserId(), request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a notification by id")
    @ApiResponse(responseCode = "404", description = "Not found or wrong owner",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<NotificationResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationService.getById(SecurityUtils.currentUserId(), id));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read (idempotent)")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationService.markRead(SecurityUtils.currentUserId(), id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a notification")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        notificationService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
