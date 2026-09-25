package com.digitallifetwin.ai.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cors")
public record CorsProperties(String allowedOrigins) {

    public List<String> resolvedOrigins() {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return List.of("http://localhost:4200");
        }
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }
}
