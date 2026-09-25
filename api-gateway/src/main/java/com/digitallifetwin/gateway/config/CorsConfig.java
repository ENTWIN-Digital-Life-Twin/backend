package com.digitallifetwin.gateway.config;

import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    private final CorsProperties corsProperties;
    private final GlobalCorsProperties globalCorsProperties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = buildConfiguration(corsProperties.resolvedOrigins());
        return exchange -> configuration;
    }

    @PostConstruct
    void configureGatewayCors() {
        List<String> origins = corsProperties.resolvedOrigins();
        log.info("Gateway CORS allowed origins: {}", origins);
        globalCorsProperties.getCorsConfigurations().put("/**", buildConfiguration(origins));
    }

    private static CorsConfiguration buildConfiguration(List<String> origins) {
        CorsConfiguration configuration = new CorsConfiguration();
        origins.forEach(configuration::addAllowedOriginPattern);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "X-Correlation-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        return configuration;
    }
}
