package com.digitallifetwin.auth.service;

import com.digitallifetwin.auth.config.AuthProperties;
import com.digitallifetwin.auth.dto.request.SendVerificationCodeRequest;
import com.digitallifetwin.auth.dto.response.SendVerificationCodeResponse;
import com.digitallifetwin.auth.entity.EmailVerificationCode;
import com.digitallifetwin.auth.exception.EmailAlreadyExistsException;
import com.digitallifetwin.auth.exception.InvalidVerificationCodeException;
import com.digitallifetwin.auth.exception.MailNotSentException;
import com.digitallifetwin.auth.repository.EmailVerificationCodeRepository;
import com.digitallifetwin.auth.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    static final long EXPIRATION_SECONDS = 600;
    private static final String GENERIC_MESSAGE = "If this email can be used, a verification code was sent.";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailVerificationCodeRepository codeRepository;
    private final PasswordResetEmailService passwordResetEmailService;
    private final AuthProperties authProperties;

    @Transactional
    public SendVerificationCodeResponse sendCode(SendVerificationCodeRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        codeRepository.findByEmailIgnoreCaseAndUsedFalse(email).forEach(existing -> existing.setUsed(true));

        String rawCode = String.format("%06d", RANDOM.nextInt(1_000_000));
        EmailVerificationCode stored = new EmailVerificationCode();
        stored.setEmail(email);
        stored.setCodeHash(PasswordResetService.sha256(rawCode));
        stored.setExpirationDate(Instant.now().plusSeconds(EXPIRATION_SECONDS));
        stored.setUsed(false);
        codeRepository.save(stored);

        boolean emailed = passwordResetEmailService.sendVerificationCode(email, rawCode);
        if (!emailed && !authProperties.exposeResetToken()) {
            throw new MailNotSentException();
        }
        if (!emailed) {
            log.warn("Verification code issued for {} but the email was not sent", email);
        }

        if (authProperties.exposeResetToken()) {
            return new SendVerificationCodeResponse(GENERIC_MESSAGE, rawCode);
        }
        return new SendVerificationCodeResponse(GENERIC_MESSAGE, null);
    }

    @Transactional
    public void consume(String email, String rawCode) {
        String normalized = normalize(email);
        String code = rawCode == null ? "" : rawCode.trim();
        EmailVerificationCode match = codeRepository
                .findByEmailIgnoreCaseAndCodeHash(normalized, PasswordResetService.sha256(code))
                .filter(EmailVerificationCode::isUsable)
                .orElseThrow(InvalidVerificationCodeException::new);
        match.setUsed(true);
        codeRepository.save(match);
    }

    static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
