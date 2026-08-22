package com.digitallifetwin.wellness.support;

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

    public static String token(UUID userId, String email, List<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(key)
                .compact();
    }

    public static String bearer(UUID userId) {
        return "Bearer " + token(userId, userId + "@example.com", List.of("USER"));
    }
}
