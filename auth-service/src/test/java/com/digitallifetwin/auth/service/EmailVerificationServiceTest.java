package com.digitallifetwin.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitallifetwin.auth.config.AuthProperties;
import com.digitallifetwin.auth.dto.request.SendVerificationCodeRequest;
import com.digitallifetwin.auth.entity.EmailVerificationCode;
import com.digitallifetwin.auth.exception.EmailAlreadyExistsException;
import com.digitallifetwin.auth.exception.InvalidVerificationCodeException;
import com.digitallifetwin.auth.exception.MailNotSentException;
import com.digitallifetwin.auth.repository.EmailVerificationCodeRepository;
import com.digitallifetwin.auth.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    private static final AuthProperties EXPOSE = new AuthProperties(
            true, 3600, "http://app.example", "noreply@entwin.test");
    private static final AuthProperties HIDE = new AuthProperties(
            false, 3600, "http://app.example", "noreply@entwin.test");

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailVerificationCodeRepository codeRepository;
    @Mock
    private PasswordResetEmailService passwordResetEmailService;

    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(userRepository, codeRepository, passwordResetEmailService, EXPOSE);
    }

    @Test
    void sendCode_rejectsExistingEmail() {
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.sendCode(new SendVerificationCodeRequest("Ada@Example.com")))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(codeRepository, never()).save(any());
    }

    @Test
    void sendCode_exposesCodeWhenSmtpMissingInLocalMode() {
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(codeRepository.findByEmailIgnoreCaseAndUsedFalse("ada@example.com")).thenReturn(List.of());
        when(passwordResetEmailService.sendVerificationCode(eq("ada@example.com"), any())).thenReturn(false);
        when(codeRepository.save(any(EmailVerificationCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.sendCode(new SendVerificationCodeRequest("Ada@Example.com"));

        assertThat(response.verificationCode()).hasSize(6);
        ArgumentCaptor<EmailVerificationCode> captor = ArgumentCaptor.forClass(EmailVerificationCode.class);
        verify(codeRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("ada@example.com");
        assertThat(captor.getValue().getExpirationDate()).isAfter(Instant.now());
    }

    @Test
    void sendCode_failsClosedWhenSmtpMissingInProduction() {
        EmailVerificationService hidden = new EmailVerificationService(
                userRepository, codeRepository, passwordResetEmailService, HIDE);
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(codeRepository.findByEmailIgnoreCaseAndUsedFalse("ada@example.com")).thenReturn(List.of());
        when(passwordResetEmailService.sendVerificationCode(eq("ada@example.com"), any())).thenReturn(false);
        when(codeRepository.save(any(EmailVerificationCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> hidden.sendCode(new SendVerificationCodeRequest("ada@example.com")))
                .isInstanceOf(MailNotSentException.class);
    }

    @Test
    void consume_rejectsUnknownCode() {
        when(codeRepository.findByEmailIgnoreCaseAndCodeHash(eq("ada@example.com"), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consume("ada@example.com", "000000"))
                .isInstanceOf(InvalidVerificationCodeException.class);
    }
}
