package com.digitallifetwin.auth.config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
        boolean exposeResetToken,
        long resetExpirationSeconds,
        String frontendUrl,
        String mailFrom
) {
    public boolean frontendUrlConfigured() {
        return frontendUrl != null && !frontendUrl.isBlank();
    }

    public boolean mailFromConfigured() {
        return mailFrom != null && !mailFrom.isBlank();
    }

    public String resetLink(String rawToken) {
        if (!frontendUrlConfigured()) {
            throw new IllegalStateException("FRONTEND_URL is not configured");
        }
        String base = frontendUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/reset-password?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }
}
