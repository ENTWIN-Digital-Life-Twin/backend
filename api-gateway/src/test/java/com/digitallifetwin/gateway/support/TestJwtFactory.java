package com.digitallifetwin.gateway.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;

public final class TestJwtFactory {

    public static final String SECRET = "LocalDevOnlyChangeMe_NeedAtLeast32Bytes!!";

    private TestJwtFactory() {
    }

    public static String validToken() {
        return token(UUID.randomUUID(), Instant.now().plusSeconds(3600), SECRET);
    }

    public static String expiredToken() {
        return token(UUID.randomUUID(), Instant.now().minusSeconds(60), SECRET);
    }

    public static String tokenWithSecret(String secret) {
        return token(UUID.randomUUID(), Instant.now().plusSeconds(3600), secret);
    }

    public static String token(UUID userId, Instant expiration, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now().minusSeconds(5);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId.toString())
                .claim("email", "user@example.com")
                .claim("roles", List.of("USER"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact();
    }
}
