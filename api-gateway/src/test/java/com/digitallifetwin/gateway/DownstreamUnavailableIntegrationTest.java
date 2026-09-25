package com.digitallifetwin.gateway;

import com.digitallifetwin.gateway.support.TestJwtFactory;
import java.io.IOException;
import java.net.ServerSocket;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class DownstreamUnavailableIntegrationTest {

    private static final MockWebServer authServer;
    private static final int CLOSED_PLANNING_PORT;

    static {
        try {
            authServer = new MockWebServer();
            authServer.start();
            try (ServerSocket socket = new ServerSocket(0)) {
                CLOSED_PLANNING_PORT = socket.getLocalPort();
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @AfterAll
    static void stopServers() throws IOException {
        authServer.shutdown();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("AUTH_SERVICE_URL", () -> authServer.url("/").toString().replaceAll("/$", ""));
        registry.add("PLANNING_SERVICE_URL", () -> "http://127.0.0.1:" + CLOSED_PLANNING_PORT);
        registry.add("WELLNESS_SERVICE_URL", () -> "http://127.0.0.1:" + CLOSED_PLANNING_PORT);
        registry.add("NOTIFICATION_SERVICE_URL", () -> "http://127.0.0.1:" + CLOSED_PLANNING_PORT);
        registry.add("AI_SERVICE_URL", () -> "http://127.0.0.1:" + CLOSED_PLANNING_PORT);
        registry.add("jwt.secret", () -> TestJwtFactory.SECRET);
        registry.add("cors.allowed-origins", () -> "http://localhost:4200");
    }

    @Test
    void unavailablePlanningReturnsClean503() {
        webTestClient.get().uri("/api/v1/tasks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.validToken())
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().exists("X-Correlation-Id")
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.error").isEqualTo("Service Unavailable")
                .jsonPath("$.message").isEqualTo("The requested service is temporarily unavailable")
                .jsonPath("$.correlationId").exists()
                .jsonPath("$.path").isEqualTo("/api/v1/tasks")
                .consumeWith(result -> {
                    byte[] raw = result.getResponseBodyContent();
                    String body = raw == null ? "" : new String(raw);
                    assertFalse(body.contains("Exception"));
                    assertFalse(body.toLowerCase().contains("stack"));
                    assertFalse(body.contains("127.0.0.1"));
                });
    }

    @Test
    void authStillWorksWhenPlanningIsDown() {
        authServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{}"));

        webTestClient.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();
    }
}
