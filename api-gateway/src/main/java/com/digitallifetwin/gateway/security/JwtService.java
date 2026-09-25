package com.digitallifetwin.gateway.security;

import com.digitallifetwin.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Edge JWT validation using the shared HMAC secret.
 * Claims-only — does not query auth-service or any service database.
 * A user disabled after token issuance may retain access until the access token expires.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public UUID extractUserId(String token) {
        Claims claims = parseClaims(token);
        String userId = claims.get("userId", String.class);
        if (userId == null || userId.isBlank()) {
            userId = claims.getSubject();
        }
        if (userId == null || userId.isBlank()) {
            throw new JwtException("Missing userId claim");
        }
        return UUID.fromString(userId);
    }

    public List<String> extractRoles(String token) {
        Object roles = parseClaims(token).get("roles");
        if (roles instanceof List<?> roleList) {
            return roleList.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
