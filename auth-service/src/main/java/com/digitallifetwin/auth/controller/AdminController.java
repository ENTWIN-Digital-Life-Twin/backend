package com.digitallifetwin.auth.controller;

import com.digitallifetwin.auth.dto.request.UpdateAccountStatusRequest;
import com.digitallifetwin.auth.dto.request.UpdateUserRoleRequest;
import com.digitallifetwin.auth.dto.response.AdminStatsResponse;
import com.digitallifetwin.auth.dto.response.ContactMessageResponse;
import com.digitallifetwin.auth.dto.response.UserProfileResponse;
import com.digitallifetwin.auth.security.SecurityUtils;
import com.digitallifetwin.auth.service.AdminUserService;
import com.digitallifetwin.auth.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/admin")
@RequiredArgsConstructor
@Tag(name = "Admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminUserService adminUserService;
    private final ContactService contactService;

    @GetMapping("/users")
    @Operation(summary = "List all users")
    public ResponseEntity<List<UserProfileResponse>> listUsers() {
        return ResponseEntity.ok(adminUserService.listUsers());
    }

    @GetMapping("/stats")
    @Operation(summary = "Platform user and contact counts")
    public ResponseEntity<AdminStatsResponse> stats() {
        return ResponseEntity.ok(adminUserService.stats());
    }

    @GetMapping("/contacts")
    @Operation(summary = "List submitted contact messages")
    public ResponseEntity<List<ContactMessageResponse>> contacts() {
        return ResponseEntity.ok(contactService.list());
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Enable or disable a user account")
    public ResponseEntity<UserProfileResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateAccountStatusRequest request) {
        return ResponseEntity.ok(adminUserService.updateStatus(SecurityUtils.currentUserId(), id, request));
    }

    @PatchMapping("/users/{id}/role")
    @Operation(summary = "Grant or revoke administrator access")
    public ResponseEntity<UserProfileResponse> updateRole(
            @PathVariable UUID id, @Valid @RequestBody UpdateUserRoleRequest request) {
        return ResponseEntity.ok(adminUserService.updateRole(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete a user account")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        adminUserService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/contacts/{id}")
    @Operation(summary = "Delete a contact message")
    public ResponseEntity<Void> deleteContact(@PathVariable UUID id) {
        contactService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
