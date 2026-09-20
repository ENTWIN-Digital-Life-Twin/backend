package com.digitallifetwin.auth.controller;

import com.digitallifetwin.auth.dto.request.ChangePasswordRequest;
import com.digitallifetwin.auth.dto.request.UpdateAssistantConversationsRequest;
import com.digitallifetwin.auth.dto.request.UpdatePreferencesRequest;
import com.digitallifetwin.auth.dto.request.UpdateProfileRequest;
import com.digitallifetwin.auth.dto.response.AssistantConversationsResponse;
import com.digitallifetwin.auth.dto.response.MessageResponse;
import com.digitallifetwin.auth.dto.response.PreferenceResponse;
import com.digitallifetwin.auth.dto.response.UserProfileResponse;
import com.digitallifetwin.auth.security.SecurityUtils;
import com.digitallifetwin.auth.service.PreferenceService;
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
    private final PreferenceService preferenceService;

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

    @GetMapping("/me/preferences")
    @Operation(summary = "Get UI and wellness preferences")
    public ResponseEntity<PreferenceResponse> getPreferences() {
        return ResponseEntity.ok(preferenceService.getPreferences(SecurityUtils.currentUserId()));
    }

    @PutMapping("/me/preferences")
    @Operation(summary = "Update UI and wellness preferences")
    public ResponseEntity<PreferenceResponse> updatePreferences(@RequestBody UpdatePreferencesRequest request) {
        return ResponseEntity.ok(preferenceService.updatePreferences(SecurityUtils.currentUserId(), request));
    }

    @GetMapping("/me/assistant-conversations")
    @Operation(summary = "Get persisted assistant conversations")
    public ResponseEntity<AssistantConversationsResponse> getConversations() {
        return ResponseEntity.ok(preferenceService.getConversations(SecurityUtils.currentUserId()));
    }

    @PutMapping("/me/assistant-conversations")
    @Operation(summary = "Replace persisted assistant conversations")
    public ResponseEntity<AssistantConversationsResponse> saveConversations(
            @RequestBody UpdateAssistantConversationsRequest request) {
        return ResponseEntity.ok(preferenceService.saveConversations(
                SecurityUtils.currentUserId(), request.conversations()));
    }
}
