package com.digitallifetwin.wellness.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public record AiProperties(
        String serviceUrl,
        int connectTimeoutMs,
        int readTimeoutMs
) {

    public String resolvedServiceUrl() {
        return serviceUrl == null || serviceUrl.isBlank() ? "http://localhost:8090" : serviceUrl;
    }

    public int resolvedConnectTimeoutMs() {
        return connectTimeoutMs > 0 ? connectTimeoutMs : 2000;
    }

    public int resolvedReadTimeoutMs() {
        return readTimeoutMs > 0 ? readTimeoutMs : 5000;
    }
}
