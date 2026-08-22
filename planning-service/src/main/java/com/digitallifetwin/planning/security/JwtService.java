package com.digitallifetwin.planning.security;

import com.digitallifetwin.planning.config.JwtProperties;
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
 * Validates access tokens issued by auth-service. Does not query auth_db.
 * Account status / role revocation until token expiry is a known microservice tradeoff.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        String userId = parseClaims(token).get("userId", String.class);
        if (userId == null) {
            userId = parseClaims(token).getSubject();
        }
        return UUID.fromString(userId);
    }

    public String extractEmail(String token) {
        return parseClaims(token).get("email", String.class);
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
