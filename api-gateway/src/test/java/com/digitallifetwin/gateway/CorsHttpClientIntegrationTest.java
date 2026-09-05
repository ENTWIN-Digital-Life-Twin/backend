package com.digitallifetwin.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CorsHttpClientIntegrationTest {

    private static final MockWebServer authServer = new MockWebServer();

    static {
        try {
            authServer.start();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @LocalServerPort
    private int port;

    @AfterAll
    static void stop() throws Exception {
        authServer.shutdown();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("AUTH_SERVICE_URL", () -> authServer.url("/").toString().replaceAll("/$", ""));
        registry.add("PLANNING_SERVICE_URL", () -> "http://127.0.0.1:9");
        registry.add("WELLNESS_SERVICE_URL", () -> "http://127.0.0.1:9");
        registry.add("NOTIFICATION_SERVICE_URL", () -> "http://127.0.0.1:9");
        registry.add("jwt.secret", () -> "LocalDevOnlyChangeMe_NeedAtLeast32Bytes!!");
        registry.add("cors.allowed-origins", () -> "http://localhost:4200");
    }

    @Test
    void preflightAndLoginWithJdkHttpClient() throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

        HttpRequest preflight = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/v1/tasks"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type")
                .build();

        HttpResponse<String> preflightResponse = client.send(preflight, HttpResponse.BodyHandlers.ofString());
        System.out.println("preflight status=" + preflightResponse.statusCode());
        System.out.println("preflight headers=" + preflightResponse.headers().map());
        assertEquals(200, preflightResponse.statusCode());
        assertTrue(preflightResponse.headers().firstValue("Access-Control-Allow-Origin").isPresent());

        authServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{}"));

        HttpRequest login = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/auth/login"))
                .header("Origin", "http://localhost:4200")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> loginResponse = client.send(login, HttpResponse.BodyHandlers.ofString());
        System.out.println("login status=" + loginResponse.statusCode());
        System.out.println("login headers=" + loginResponse.headers().map());
        assertEquals(200, loginResponse.statusCode());
        assertEquals("http://localhost:4200",
                loginResponse.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
    }

    @Test
    void unconfiguredOriginRejectedOnPreflight() throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

        HttpRequest preflight = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/v1/tasks"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://evil.example")
                .header("Access-Control-Request-Method", "GET")
                .build();

        HttpResponse<String> response = client.send(preflight, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());
    }
}
