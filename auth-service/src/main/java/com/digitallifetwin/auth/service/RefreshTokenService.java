package com.digitallifetwin.auth.service;

import com.digitallifetwin.auth.entity.RefreshToken;
import com.digitallifetwin.auth.entity.User;
import com.digitallifetwin.auth.exception.InvalidRefreshTokenException;
import com.digitallifetwin.auth.repository.RefreshTokenRepository;
import com.digitallifetwin.auth.security.JwtService;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Transactional
    public RefreshToken issue(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(generateTokenValue());
        refreshToken.setExpirationDate(Instant.now().plusSeconds(jwtService.getRefreshExpirationSeconds()));
        refreshToken.setRevoked(false);
        refreshToken.setCreatedAt(Instant.now());
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken requireUsable(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenWithUser(token)
                .orElseThrow(InvalidRefreshTokenException::new);
        if (!refreshToken.isUsable()) {
            throw new InvalidRefreshTokenException();
        }
        return refreshToken;
    }

    @Transactional
    public RefreshToken rotate(String currentToken) {
        RefreshToken existing = requireUsable(currentToken);
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);
        return issue(existing.getUser());
    }

    @Transactional
    public void revoke(String token, UUID currentUserId) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenWithUser(token)
                .orElseThrow(InvalidRefreshTokenException::new);
        if (!refreshToken.getUser().getId().equals(currentUserId)) {
            throw new InvalidRefreshTokenException();
        }
        if (!refreshToken.isRevoked()) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
        }
    }

    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.revokeAllActiveByUser(user);
    }

    private String generateTokenValue() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
