package com.digitallifetwin.auth.controller;

import com.digitallifetwin.auth.dto.request.ContactRequest;
import com.digitallifetwin.auth.dto.request.ForgotPasswordRequest;
import com.digitallifetwin.auth.dto.request.GoogleLoginRequest;
import com.digitallifetwin.auth.dto.request.LoginRequest;
import com.digitallifetwin.auth.dto.request.LogoutRequest;
import com.digitallifetwin.auth.dto.request.RefreshTokenRequest;
import com.digitallifetwin.auth.dto.request.RegisterRequest;
import com.digitallifetwin.auth.dto.request.ResetPasswordRequest;
import com.digitallifetwin.auth.dto.request.SendVerificationCodeRequest;
import com.digitallifetwin.auth.dto.response.AuthResponse;
import com.digitallifetwin.auth.dto.response.ForgotPasswordResponse;
import com.digitallifetwin.auth.dto.response.MessageResponse;
import com.digitallifetwin.auth.dto.response.SendVerificationCodeResponse;
import com.digitallifetwin.auth.dto.response.UserResponse;
import com.digitallifetwin.auth.security.SecurityUtils;
import com.digitallifetwin.auth.service.AuthService;
import com.digitallifetwin.auth.service.ContactService;
import com.digitallifetwin.auth.service.EmailVerificationService;
import com.digitallifetwin.auth.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final ContactService contactService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register/send-code")
    @SecurityRequirements
    @Operation(summary = "Send a 6-digit SMTP code before creating an account")
    public ResponseEntity<SendVerificationCodeResponse> sendRegisterCode(
            @Valid @RequestBody SendVerificationCodeRequest request) {
        return ResponseEntity.ok(emailVerificationService.sendCode(request));
    }

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(summary = "Register a new user account")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Authenticate and receive access and refresh tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/google")
    @SecurityRequirements
    @Operation(summary = "Authenticate with a verified Google ID token")
    public ResponseEntity<AuthResponse> google(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithGoogle(request));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Rotate a refresh token and issue a new access token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the supplied refresh token")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request, SecurityUtils.currentUserId());
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    @SecurityRequirements
    @Operation(summary = "Request a password reset token")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    @SecurityRequirements
    @Operation(summary = "Reset password with a one-time token")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.resetPassword(request));
    }

    @PostMapping("/contact")
    @SecurityRequirements
    @Operation(summary = "Submit a public contact message")
    public ResponseEntity<MessageResponse> contact(@Valid @RequestBody ContactRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contactService.submit(request));
    }
}
