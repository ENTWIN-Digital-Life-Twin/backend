package com.digitallifetwin.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitallifetwin.auth.config.AuthProperties;
import com.digitallifetwin.auth.dto.request.ForgotPasswordRequest;
import com.digitallifetwin.auth.dto.request.ResetPasswordRequest;
import com.digitallifetwin.auth.entity.PasswordResetToken;
import com.digitallifetwin.auth.entity.User;
import com.digitallifetwin.auth.exception.InvalidResetTokenException;
import com.digitallifetwin.auth.repository.PasswordResetTokenRepository;
import com.digitallifetwin.auth.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final AuthProperties EXPOSE_TOKEN = new AuthProperties(
            true, 3600, "http://app.example", "noreply@entwin.test");
    private static final AuthProperties HIDE_TOKEN = new AuthProperties(
            false, 3600, "http://app.example", "noreply@entwin.test");

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private PasswordResetEmailService passwordResetEmailService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("jane@example.com");
        user.setPasswordHash("old");
    }

    private PasswordResetService service(AuthProperties properties) {
        return new PasswordResetService(
                userRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                refreshTokenService,
                properties,
                passwordResetEmailService);
    }

    @Test
    void forgotPassword_unknownEmail_doesNotCreateTokenOrSendEmail() {
        when(userRepository.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

        var response = service(EXPOSE_TOKEN).forgotPassword(new ForgotPasswordRequest("ghost@example.com"));

        assertThat(response.message()).contains("If an account exists");
        assertThat(response.resetToken()).isNull();
        verify(passwordResetTokenRepository, never()).save(any());
        verify(passwordResetEmailService, never()).sendResetEmail(anyString(), anyString());
    }

    @Test
    void forgotPassword_knownEmail_generatesHashedExpiringTokenAndSendsEmail() {
        stubKnownUser();

        var response = service(EXPOSE_TOKEN).forgotPassword(new ForgotPasswordRequest("jane@example.com"));

        assertThat(response.resetToken()).isNotBlank();
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        PasswordResetToken stored = captor.getValue();
        assertThat(stored.getTokenHash()).isEqualTo(PasswordResetService.sha256(response.resetToken()));
        assertThat(stored.getExpirationDate()).isAfter(Instant.now());
        assertThat(stored.isUsed()).isFalse();
        verify(passwordResetEmailService).sendResetEmail(eq("jane@example.com"), eq(response.resetToken()));
    }

    @Test
    void forgotPassword_exposeResetTokenFalse_doesNotReturnTokenButStillEmails() {
        stubKnownUser();
        when(passwordResetEmailService.sendResetEmail(eq("jane@example.com"), anyString())).thenReturn(true);

        var response = service(HIDE_TOKEN).forgotPassword(new ForgotPasswordRequest("jane@example.com"));

        assertThat(response.message()).contains("If an account exists");
        assertThat(response.resetToken()).isNull();
        verify(passwordResetEmailService).sendResetEmail(eq("jane@example.com"), anyString());
    }

    @Test
    void forgotPassword_smtpFailure_stillReturnsGenericResponse() {
        stubKnownUser();
        when(passwordResetEmailService.sendResetEmail(eq("jane@example.com"), anyString())).thenReturn(false);

        var response = service(HIDE_TOKEN).forgotPassword(new ForgotPasswordRequest("jane@example.com"));

        assertThat(response.message()).contains("If an account exists");
        assertThat(response.resetToken()).isNull();
    }

    @Test
    void resetPassword_rejectsUnknownToken() {
        when(passwordResetTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service(HIDE_TOKEN).resetPassword(
                new ResetPasswordRequest("missing", "NewPassword123!")))
                .isInstanceOf(InvalidResetTokenException.class);
        verify(refreshTokenService, never()).revokeAllForUser(any());
    }

    @Test
    void resetPassword_rejectsExpiredToken() {
        PasswordResetToken token = usableToken();
        token.setExpirationDate(Instant.now().minusSeconds(60));
        when(passwordResetTokenRepository.findByTokenHash(PasswordResetService.sha256("raw-token")))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service(HIDE_TOKEN).resetPassword(
                new ResetPasswordRequest("raw-token", "NewPassword123!")))
                .isInstanceOf(InvalidResetTokenException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_rejectsAlreadyUsedToken() {
        PasswordResetToken token = usableToken();
        token.setUsed(true);
        when(passwordResetTokenRepository.findByTokenHash(PasswordResetService.sha256("raw-token")))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service(HIDE_TOKEN).resetPassword(
                new ResetPasswordRequest("raw-token", "NewPassword123!")))
                .isInstanceOf(InvalidResetTokenException.class);
    }

    @Test
    void resetPassword_updatesHashInvalidatesTokenAndRevokesSessions() {
        PasswordResetToken token = usableToken();
        when(passwordResetTokenRepository.findByTokenHash(PasswordResetService.sha256("raw-token")))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("new-hash");
        when(userRepository.save(user)).thenReturn(user);
        when(passwordResetTokenRepository.save(token)).thenReturn(token);

        service(HIDE_TOKEN).resetPassword(new ResetPasswordRequest("raw-token", "NewPassword123!"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(token.isUsed()).isTrue();
        verify(refreshTokenService).revokeAllForUser(user);
    }

    private void stubKnownUser() {
        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserAndUsedFalse(user)).thenReturn(List.of());
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private PasswordResetToken usableToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setUsed(false);
        token.setExpirationDate(Instant.now().plusSeconds(600));
        return token;
    }
}
