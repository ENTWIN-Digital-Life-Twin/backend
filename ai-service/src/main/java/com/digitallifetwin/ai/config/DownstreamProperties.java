package com.digitallifetwin.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "downstream")
public record DownstreamProperties(
        String planningServiceUrl,
        String wellnessServiceUrl,
        int connectTimeoutMs,
        int readTimeoutMs
) {

    public String resolvedPlanningServiceUrl() {
        return planningServiceUrl == null || planningServiceUrl.isBlank()
                ? "http://localhost:8082" : planningServiceUrl;
    }

    public String resolvedWellnessServiceUrl() {
        return wellnessServiceUrl == null || wellnessServiceUrl.isBlank()
                ? "http://localhost:8083" : wellnessServiceUrl;
    }

    public int resolvedConnectTimeoutMs() {
        return connectTimeoutMs > 0 ? connectTimeoutMs : 2000;
    }

    public int resolvedReadTimeoutMs() {
        return readTimeoutMs > 0 ? readTimeoutMs : 4000;
    }
}
