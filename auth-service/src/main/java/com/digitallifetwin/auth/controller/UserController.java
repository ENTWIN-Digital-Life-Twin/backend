package com.digitallifetwin.auth.controller;

import com.digitallifetwin.auth.dto.request.ChangePasswordRequest;
import com.digitallifetwin.auth.dto.request.UpdateProfileRequest;
import com.digitallifetwin.auth.dto.response.MessageResponse;
import com.digitallifetwin.auth.dto.response.UserProfileResponse;
import com.digitallifetwin.auth.security.SecurityUtils;
import com.digitallifetwin.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user profile")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser(SecurityUtils.currentUserId()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the authenticated user profile")
    public ResponseEntity<UserProfileResponse> updateCurrentUser(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateCurrentUser(SecurityUtils.currentUserId(), request));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change the authenticated user's password")
    public ResponseEntity<MessageResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(userService.changePassword(SecurityUtils.currentUserId(), request));
    }
}
