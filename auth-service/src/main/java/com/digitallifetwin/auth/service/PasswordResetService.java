package com.digitallifetwin.auth.service;

import com.digitallifetwin.auth.config.AuthProperties;
import com.digitallifetwin.auth.dto.request.ForgotPasswordRequest;
import com.digitallifetwin.auth.dto.request.ResetPasswordRequest;
import com.digitallifetwin.auth.dto.response.ForgotPasswordResponse;
import com.digitallifetwin.auth.dto.response.MessageResponse;
import com.digitallifetwin.auth.entity.PasswordResetToken;
import com.digitallifetwin.auth.entity.User;
import com.digitallifetwin.auth.exception.InvalidResetTokenException;
import com.digitallifetwin.auth.repository.PasswordResetTokenRepository;
import com.digitallifetwin.auth.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String GENERIC_MESSAGE = "If an account exists for this email, a reset link was sent.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuthProperties authProperties;
    private final PasswordResetEmailService passwordResetEmailService;

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().trim();
        return userRepository.findByEmailIgnoreCase(email)
                .map(this::issueToken)
                .orElseGet(() -> new ForgotPasswordResponse(GENERIC_MESSAGE, null));
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(sha256(request.token()))
                .filter(PasswordResetToken::isUsable)
                .orElseThrow(InvalidResetTokenException::new);

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        token.setUsed(true);
        passwordResetTokenRepository.save(token);
        refreshTokenService.revokeAllForUser(user);
        return new MessageResponse("Password updated successfully");
    }

    private ForgotPasswordResponse issueToken(User user) {
        passwordResetTokenRepository.findByUserAndUsedFalse(user).forEach(existing -> existing.setUsed(true));

        String rawToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(sha256(rawToken));
        token.setExpirationDate(Instant.now().plusSeconds(authProperties.resetExpirationSeconds()));
        token.setUsed(false);
        passwordResetTokenRepository.save(token);

        boolean emailed = passwordResetEmailService.sendResetEmail(user.getEmail(), rawToken);
        if (!emailed) {
            log.warn("Password reset issued for {} but the reset email was not sent", user.getEmail());
        }

        if (authProperties.exposeResetToken()) {
            log.info("Password reset issued for {} (token exposed for local development)", user.getEmail());
            return new ForgotPasswordResponse(GENERIC_MESSAGE, rawToken);
        }
        log.info("Password reset issued for {}", user.getEmail());
        return new ForgotPasswordResponse(GENERIC_MESSAGE, null);
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
