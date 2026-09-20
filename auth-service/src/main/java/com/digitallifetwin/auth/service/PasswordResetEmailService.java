package com.digitallifetwin.auth.service;

import com.digitallifetwin.auth.config.AuthProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetEmailService {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final AuthProperties authProperties;

    public boolean sendVerificationCode(String to, String code) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!canSendVerification(sender)) {
            return false;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(authProperties.mailFrom().trim());
            helper.setTo(to);
            helper.setSubject("Your ENTWIN verification code");
            helper.setText(verificationBody(code), false);
            sender.send(message);
            return true;
        } catch (Exception ex) {
            log.warn("Failed to send verification email to {}", to);
            return false;
        }
    }

    public boolean sendResetEmail(String to, String rawToken) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!canSend(sender)) {
            return false;
        }
        try {
            String link = authProperties.resetLink(rawToken);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(authProperties.mailFrom().trim());
            helper.setTo(to);
            helper.setSubject("Reset your ENTWIN password");
            helper.setText(body(link), false);
            sender.send(message);
            return true;
        } catch (Exception ex) {
            log.warn("Failed to send password reset email to {}", to);
            return false;
        }
    }

    boolean canSend(JavaMailSender sender) {
        if (sender == null) {
            return false;
        }
        if (sender instanceof JavaMailSenderImpl impl) {
            String host = impl.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }
        }
        return authProperties.mailFromConfigured() && authProperties.frontendUrlConfigured();
    }

    boolean canSendVerification(JavaMailSender sender) {
        if (sender == null) {
            return false;
        }
        if (sender instanceof JavaMailSenderImpl impl) {
            String host = impl.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }
        }
        return authProperties.mailFromConfigured();
    }

    private String verificationBody(String code) {
        return """
                ENTWIN

                Your email verification code is:

                %s

                Enter this code to finish creating your account. It expires in 10 minutes.

                If you did not request this, ignore this email.
                """.formatted(code);
    }

    private String body(String resetLink) {
        return """
                ENTWIN

                You requested a password reset for your ENTWIN account.

                Open this link to choose a new password:
                %s

                This link expires in %s and can be used only once.

                If you did not request this, ignore this email. Your password will stay the same.
                """.formatted(resetLink, expiryLabel());
    }

    private String expiryLabel() {
        long seconds = Math.max(1, authProperties.resetExpirationSeconds());
        if (seconds >= 3600) {
            long hours = seconds / 3600;
            return hours == 1 ? "1 hour" : hours + " hours";
        }
        long minutes = Math.max(1, seconds / 60);
        return minutes == 1 ? "1 minute" : minutes + " minutes";
    }
}
