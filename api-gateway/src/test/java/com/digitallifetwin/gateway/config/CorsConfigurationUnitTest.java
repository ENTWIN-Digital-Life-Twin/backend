package com.digitallifetwin.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

class CorsConfigurationUnitTest {

    @Test
    void allowsConfiguredAngularOriginWithCredentials() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        assertEquals("http://localhost:4200", configuration.checkOrigin("http://localhost:4200"));
        assertNotNull(configuration.checkHttpMethod(org.springframework.http.HttpMethod.GET));
        assertNotNull(configuration.checkHeaders(List.of("Authorization", "Content-Type")));
    }
}
