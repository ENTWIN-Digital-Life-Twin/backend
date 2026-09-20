package com.digitallifetwin.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google")
public record GoogleProperties(String clientId) {

    public boolean configured() {
        return clientId != null && !clientId.isBlank();
    }
}
