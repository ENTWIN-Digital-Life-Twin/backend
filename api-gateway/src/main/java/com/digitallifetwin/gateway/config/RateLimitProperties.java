package com.digitallifetwin.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.rate-limit")
public record RateLimitProperties(Auth auth) {

    public RateLimitProperties {
        if (auth == null) {
            auth = new Auth(20, 60);
        } else {
            if (auth.requests() <= 0) {
                auth = new Auth(20, auth.windowSeconds());
            }
            if (auth.windowSeconds() <= 0) {
                auth = new Auth(auth.requests(), 60);
            }
        }
    }

    public record Auth(int requests, int windowSeconds) {
    }
}
