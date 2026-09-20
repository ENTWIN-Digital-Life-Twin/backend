package com.digitallifetwin.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitallifetwin.auth.config.AuthProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@ExtendWith(MockitoExtension.class)
class PasswordResetEmailServiceTest {

    private static final AuthProperties CONFIGURED = new AuthProperties(
            false, 3600, "http://app.example", "noreply@entwin.test");

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock
    private JavaMailSender mailSender;

    private PasswordResetEmailService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetEmailService(mailSenderProvider, CONFIGURED);
    }

    @Test
    void sendResetEmail_skipsWhenMailSenderMissing() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        assertThat(service.sendResetEmail("jane@example.com", "raw-token")).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendResetEmail_skipsWhenSmtpHostBlank() {
        JavaMailSenderImpl blankHost = new JavaMailSenderImpl();
        blankHost.setHost("");
        when(mailSenderProvider.getIfAvailable()).thenReturn(blankHost);

        assertThat(service.sendResetEmail("jane@example.com", "raw-token")).isFalse();
    }

    @Test
    void sendResetEmail_skipsWhenMailFromBlank() {
        PasswordResetEmailService missingFrom = new PasswordResetEmailService(
                mailSenderProvider, new AuthProperties(false, 3600, "http://app.example", ""));
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);

        assertThat(missingFrom.sendResetEmail("jane@example.com", "raw-token")).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendResetEmail_skipsWhenFrontendUrlBlank() {
        PasswordResetEmailService missingFrontend = new PasswordResetEmailService(
                mailSenderProvider, new AuthProperties(false, 3600, "", "noreply@entwin.test"));
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);

        assertThat(missingFrontend.sendResetEmail("jane@example.com", "raw-token")).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendResetEmail_sendsBrandedLinkWithoutThrowing() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(message);

        assertThat(service.sendResetEmail("jane@example.com", "raw-token")).isTrue();

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        String content = String.valueOf(captor.getValue().getContent());
        assertThat(captor.getValue().getSubject()).isEqualTo("Reset your ENTWIN password");
        assertThat(content).contains("ENTWIN");
        assertThat(content).contains("http://app.example/reset-password?token=raw-token");
        assertThat(content).contains("expires");
        assertThat(content).contains("If you did not request this");
    }

    @Test
    void sendResetEmail_smtpFailure_returnsFalseWithoutPropagating() {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        assertThat(service.sendResetEmail("jane@example.com", "raw-token")).isFalse();
    }

    @Test
    void sendVerificationCode_sendsSixDigitCodeWithoutFrontendUrl() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(message);

        assertThat(service.sendVerificationCode("ada@example.com", "482913")).isTrue();

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        String content = String.valueOf(captor.getValue().getContent());
        assertThat(captor.getValue().getSubject()).isEqualTo("Your ENTWIN verification code");
        assertThat(content).contains("482913");
        assertThat(content).contains("10 minutes");
    }

    @Test
    void sendVerificationCode_skipsWhenSmtpHostBlank() {
        JavaMailSenderImpl blankHost = new JavaMailSenderImpl();
        blankHost.setHost("");
        when(mailSenderProvider.getIfAvailable()).thenReturn(blankHost);

        assertThat(service.sendVerificationCode("ada@example.com", "482913")).isFalse();
    }
}
