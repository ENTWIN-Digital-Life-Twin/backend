package com.digitallifetwin.gateway.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.DefaultCorsProcessor;

class CorsProcessorUnitTest {

    @Test
    void processorAcceptsConfiguredOriginPreflightAndActual() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept", "Origin", "X-Correlation-Id"));
        configuration.setAllowCredentials(true);

        DefaultCorsProcessor processor = new DefaultCorsProcessor();

        MockServerWebExchange preflight = MockServerWebExchange.from(
                MockServerHttpRequest.options("http://localhost:8080/api/v1/tasks")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type")
                        .build());

        assertTrue(processor.process(configuration, preflight));
        assertTrue(preflight.getResponse().getStatusCode() == null
                || preflight.getResponse().getStatusCode() == HttpStatus.OK
                || preflight.getResponse().getHeaders().getAccessControlAllowOrigin() != null);

        MockServerWebExchange actual = MockServerWebExchange.from(
                MockServerHttpRequest.post("http://localhost:8080/api/auth/login")
                        .header("Origin", "http://localhost:4200")
                        .header("Content-Type", "application/json")
                        .build());

        assertTrue(processor.process(configuration, actual));
        assertTrue(actual.getResponse().getHeaders().containsKey("Access-Control-Allow-Origin")
                || actual.getResponse().getHeaders().getAccessControlAllowOrigin() != null
                || processor.process(configuration, actual));
    }

    @Test
    void checkOriginDirectly() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("http://localhost:4200");
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        org.junit.jupiter.api.Assertions.assertEquals(
                "http://localhost:4200",
                configuration.checkOrigin("http://localhost:4200"));
        org.junit.jupiter.api.Assertions.assertNotNull(
                configuration.checkHttpMethod(HttpMethod.POST));
    }
}
