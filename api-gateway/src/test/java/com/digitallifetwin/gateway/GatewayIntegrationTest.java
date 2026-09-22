package com.digitallifetwin.gateway;

import com.digitallifetwin.gateway.filter.AuthRateLimitFilter;
import com.digitallifetwin.gateway.filter.GatewayHeaders;
import com.digitallifetwin.gateway.support.TestJwtFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.QueueDispatcher;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayIntegrationTest {

    private static final MockWebServer authServer;
    private static final MockWebServer planningServer;
    private static final MockWebServer wellnessServer;
    private static final MockWebServer notificationServer;
    private static final MockWebServer aiServer;

    static {
        try {
            authServer = new MockWebServer();
            planningServer = new MockWebServer();
            wellnessServer = new MockWebServer();
            notificationServer = new MockWebServer();
            aiServer = new MockWebServer();
            authServer.start();
            planningServer.start();
            wellnessServer.start();
            notificationServer.start();
            aiServer.start();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AuthRateLimitFilter authRateLimitFilter;

    @AfterAll
    static void stopServers() throws IOException {
        authServer.shutdown();
        planningServer.shutdown();
        wellnessServer.shutdown();
        notificationServer.shutdown();
        aiServer.shutdown();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("AUTH_SERVICE_URL", () -> authServer.url("/").toString().replaceAll("/$", ""));
        registry.add("PLANNING_SERVICE_URL", () -> planningServer.url("/").toString().replaceAll("/$", ""));
        registry.add("WELLNESS_SERVICE_URL", () -> wellnessServer.url("/").toString().replaceAll("/$", ""));
        registry.add("NOTIFICATION_SERVICE_URL", () -> notificationServer.url("/").toString().replaceAll("/$", ""));
        registry.add("AI_SERVICE_URL", () -> aiServer.url("/").toString().replaceAll("/$", ""));
        registry.add("jwt.secret", () -> TestJwtFactory.SECRET);
        registry.add("CORS_ALLOWED_ORIGINS", () -> "http://localhost:4200");
        registry.add("cors.allowed-origins", () -> "http://localhost:4200");
        registry.add("gateway.rate-limit.auth.requests", () -> "5");
        registry.add("gateway.rate-limit.auth.window-seconds", () -> "60");
    }

    @BeforeEach
    void resetState() throws InterruptedException {
        authRateLimitFilter.reset();
        resetMockServer(authServer);
        resetMockServer(planningServer);
        resetMockServer(wellnessServer);
        resetMockServer(notificationServer);
        resetMockServer(aiServer);
    }

    private static void resetMockServer(MockWebServer server) throws InterruptedException {
        server.setDispatcher(new QueueDispatcher());
        while (server.takeRequest(0, TimeUnit.MILLISECONDS) != null) {
            // drain any recorded requests from prior tests
        }
    }

    private void enqueueJson(MockWebServer server, int code, String body) {
        server.enqueue(new MockResponse()
                .setResponseCode(code)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(body));
    }

    private RecordedRequest take(MockWebServer server) throws InterruptedException {
        RecordedRequest recorded = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(recorded, "Expected a downstream request");
        return recorded;
    }

    @Nested
    class Routing {

        @Test
        void routesAuthLogin() throws InterruptedException {
            enqueueJson(authServer, 200, "{\"ok\":true}");
            webTestClient.post().uri("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"email\":\"a@b.com\",\"password\":\"secret\"}")
                    .exchange()
                    .expectStatus().isOk();
            RecordedRequest recorded = take(authServer);
            assertEquals("/api/auth/login", recorded.getPath());
        }

        @Test
        void routesUsersMe() throws InterruptedException {
            enqueueJson(authServer, 200, "{\"id\":\"1\"}");
            webTestClient.get().uri("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.validToken())
                    .exchange()
                    .expectStatus().isOk();
            RecordedRequest recorded = take(authServer);
            assertEquals("/api/users/me", recorded.getPath());
        }

        @Test
        void routesTasks() throws InterruptedException {
            enqueueJson(planningServer, 200, "[]");
            webTestClient.get().uri("/api/v1/tasks")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.validToken())
                    .exchange()
                    .expectStatus().isOk();
            RecordedRequest recorded = take(planningServer);
            assertEquals("/api/v1/tasks", recorded.getPath());
        }

        @Test
        void routesDashboardStats() throws InterruptedException {
            enqueueJson(planningServer, 200, "{}");
            webTestClient.get().uri("/api/v1/dashboard/stats")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.validToken())
                    .exchange()
                    .expectStatus().isOk();
            RecordedRequest recorded = take(planningServer);
            assertEquals("/api/v1/dashboard/stats", recorded.getPath());
        }

        @Test
        void routesEvents() throws InterruptedException {
            enqueueJson(planningServer, 200, "[]");
            webTestClient.get().uri("/api/v1/events")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.validToken())
                    .exchange()
                    .expectStatus().isOk();
            RecordedRequest recorded = take(planningServer);
            assertEquals("/api/v1/events", recorded.getPath());
        }

        @Test
        void routesPlanningDaily() throws InterruptedException {
            enqueueJson(planningServer, 200, "{}");
            webTestClient.get().uri("/api/v1/planning/daily?date=2026-09-05")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.validToken())
                    .exchange()
                    .expectStatus().isOk();
            RecordedRequest recorded = take(planningServer);
            assertTrue(recorded.getPath().startsWith("/api/v1/planning/daily"));
        }

        @Test
        void routesWellness() throws InterruptedException {
            enqueueJson(wellnessServer, 200, "{}");
            webTestClient.get().uri("/api/v1/wellness/summary/daily?date=2026-09-05")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.validToken())
                    .exchange()
                    .expectStatus().isOk();
            RecordedRequest recorded = take(wellnessServer);
            assertTrue(recorded.getPath().startsWith("/api/v1/wellness/summary/daily"));
        }

        @Test
        void routesWellnessDashboard() throws InterruptedException {
            enqueueJson(wellnessServer, 200, "{}");
            webTestClient.get().uri("/api/v1/wellness/dashboard")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.validToken())
                    .exchange()
                    .expectStatus().isOk();
            RecordedRequest recorded = take(wellnessServer);
            assertEquals("/api/v1/wellness/dashboard", recorded.getPath());
        }

        @Test
        void routesReminders() throws InterruptedException {
            enqueueJson(notificationServer, 200, "[]");
            webTestClient.get().uri("/api/v1/reminders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.validToken())
                    .exchange()
                    .expectStatus().isOk();
            RecordedRequest recorded = take(notificationServer);
            assertEquals("/api/v1/reminders", recorded.getPath());
        }

        @Test
        void routesNotifications() throws InterruptedException {
            enqueueJson(notificationServer, 200, "[]");
            webTestClient.get().uri("/api/v1/notifications")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.validToken())
                    .exchange()
                    .expectStatus().isOk();
            RecordedRequest recorded = take(notificationServer);
            assertEquals("/api/v1/notifications", recorded.getPath());
        }

        @Test
        void routesAiChat() throws InterruptedException {
            enqueueJson(aiServer, 200, "{\"answer\":\"ok\"}");
            webTestClient.post().uri("/api/v1/ai/chat")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.validToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"question\":\"How do I create a task?\"}")
                    .exchange()
                    .expectStatus().isOk();
            RecordedRequest recorded = take(aiServer);
            assertEquals("/api/v1/ai/chat", recorded.getPath());
        }

        @Test
        void routesAiLifestyleRisk() throws InterruptedException {
            enqueueJson(aiServer, 200, "{\"riskLevel\":\"LOW\"}");
            webTestClient.post().uri("/api/v1/ai/lifestyle-risk")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.validToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"averageStress\":3}")
                    .exchange()
                    .expectStatus().isOk();
            assertEquals("/api/v1/ai/lifestyle-risk", take(aiServer).getPath());
        }

        @Test
        void routesAiRecommendations() throws InterruptedException {
            enqueueJson(aiServer, 200, "{\"recommendations\":[]}");
            webTestClient.post().uri("/api/v1/ai/recommendations")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.validToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"hydrationMl\":900}")
                    .exchange()
                    .expectStatus().isOk();
            assertEquals("/api/v1/ai/recommendations", take(aiServer).getPath());
        }

        @Test
        void routesAiTaskDuration() throws InterruptedException {
            enqueueJson(aiServer, 200, "{\"predictedDurationMinutes\":55}");
            webTestClient.post().uri("/api/v1/ai/task-duration")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.validToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"userEstimateMinutes\":60}")
                    .exchange()
                    .expectStatus().isOk();
            assertEquals("/api/v1/ai/task-duration", take(aiServer).getPath());
        }

        @Test
        void routesAiSleepRisk() throws InterruptedException {
            enqueueJson(aiServer, 200, "{\"riskLevel\":\"LOW\"}");
            webTestClient.post().uri("/api/v1/ai/sleep-risk")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.validToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"age\":24}")
                    .exchange()
                    .expectStatus().isOk();
            assertEquals("/api/v1/ai/sleep-risk", take(aiServer).getPath());
        }
    }

    @Nested
    class Security {

        @Test
        void protectedAiChatWithoutJwtReturns401() {
            webTestClient.post().uri("/api/v1/ai/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"question\":\"Hello\"}")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        void publicLoginWithoutJwt() {
            enqueueJson(authServer, 200, "{}");
            webTestClient.post().uri("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{}")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        void publicRegisterWithoutJwt() {
            enqueueJson(authServer, 201, "{}");
            webTestClient.post().uri("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{}")
                    .exchange()
                    .expectStatus().isEqualTo(201);
        }

        @Test
        void publicRegisterSendCodeWithoutJwt() {
            enqueueJson(authServer, 200, "{}");
            webTestClient.post().uri("/api/auth/register/send-code")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"email\":\"ada@example.com\"}")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        void publicForgotPasswordWithoutJwt() {
            enqueueJson(authServer, 200, "{}");
            webTestClient.post().uri("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"email\":\"ada@example.com\"}")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        void publicGoogleLoginWithoutJwt() {
            enqueueJson(authServer, 200, "{}");
            webTestClient.post().uri("/api/auth/google")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"credential\":\"google-id-token\"}")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        void publicContactWithoutJwt() {
            enqueueJson(authServer, 201, "{}");
            webTestClient.post().uri("/api/auth/contact")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"name\":\"Ada\",\"email\":\"ada@example.com\",\"subject\":\"Hi\",\"message\":\"Hello there\"}")
                    .exchange()
                    .expectStatus().isEqualTo(201);
        }

        @Test
        void protectedWithoutJwtReturns401() {
            webTestClient.get().uri("/api/v1/tasks")
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                    .expectHeader().valueEquals("X-Frame-Options", "DENY")
                    .expectHeader().valueEquals("Content-Security-Policy", "frame-ancestors 'none'")
                    .expectHeader().valueEquals("Referrer-Policy", "no-referrer")
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(401)
                    .jsonPath("$.correlationId").exists();
        }

        @Test
        void malformedJwtReturns401() {
            webTestClient.get().uri("/api/v1/tasks")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        void invalidSignatureReturns401() {
            String token = TestJwtFactory.tokenWithSecret("AnotherSecret_NeedAtLeast32BytesLong!!");
            webTestClient.get().uri("/api/v1/tasks")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        void expiredJwtReturns401() {
            webTestClient.get().uri("/api/v1/tasks")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.expiredToken())
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        void validJwtForwardsAuthorizationHeader() throws InterruptedException {
            enqueueJson(planningServer, 200, "[]");
            String token = TestJwtFactory.validToken();
            webTestClient.get().uri("/api/v1/tasks")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .exchange()
                    .expectStatus().isOk();
            RecordedRequest recorded = take(planningServer);
            assertEquals("Bearer " + token, recorded.getHeader(HttpHeaders.AUTHORIZATION));
        }
    }

    @Nested
    class CorrelationId {

        @Test
        void generatesWhenMissing() {
            enqueueJson(authServer, 200, "{}");
            webTestClient.post().uri("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{}")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().exists(GatewayHeaders.CORRELATION_ID);
        }

        @Test
        void preservesValidIncomingId() throws InterruptedException {
            enqueueJson(authServer, 200, "{}");
            String correlationId = "corr-test-12345";
            webTestClient.post().uri("/api/auth/login")
                    .header(GatewayHeaders.CORRELATION_ID, correlationId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{}")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals(GatewayHeaders.CORRELATION_ID, correlationId);
            RecordedRequest recorded = take(authServer);
            assertEquals(correlationId, recorded.getHeader(GatewayHeaders.CORRELATION_ID));
        }

        @Test
        void replacesOversizedValue() {
            enqueueJson(authServer, 200, "{}");
            String oversized = "x".repeat(200);
            webTestClient.post().uri("/api/auth/login")
                    .header(GatewayHeaders.CORRELATION_ID, oversized)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{}")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().value(GatewayHeaders.CORRELATION_ID, value -> assertNotEquals(oversized, value));
        }
    }

    @Nested
    class RateLimit {

        @Test
        void exceedsLimitReturns429() {
            for (int i = 0; i < 5; i++) {
                enqueueJson(authServer, 200, "{}");
                webTestClient.post().uri("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("{}")
                        .exchange()
                        .expectStatus().isOk();
            }
            webTestClient.post().uri("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{}")
                    .exchange()
                    .expectStatus().isEqualTo(429)
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(429)
                    .jsonPath("$.correlationId").exists();
        }

        @Test
        void protectedRoutesUnaffectedByAuthLimiter() {
            enqueueJson(planningServer, 200, "[]");
            webTestClient.get().uri("/api/v1/tasks")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtFactory.validToken())
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    @Nested
    class Actuator {

        @Test
        void healthIsPublic() {
            webTestClient.get().uri("/actuator/health")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("UP");
        }
    }
}
