package com.digitallifetwin.planning.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

class PlanningAiClientTest {

    private MockWebServer server;
    private PlanningAiClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        RestClient restClient = RestClient.builder()
                .baseUrl(server.url("/").toString().replaceAll("/$", ""))
                .build();
        client = new PlanningAiClient(restClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void mapsCamelCaseTaskDurationResponse() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"predictedDurationMinutes":55,"engine":"BASELINE_ESTIMATOR","confidence":null}
                        """));

        Optional<TaskDurationAiResponse> result = client.estimateDuration(
                new TaskDurationAiRequest(null, "HARD", "HIGH", 60, 40));

        assertThat(result).isPresent();
        assertThat(result.get().predictedDurationMinutes()).isEqualTo(55);
        assertThat(result.get().engine()).isEqualTo("BASELINE_ESTIMATOR");
        assertThat(result.get().confidence()).isNull();

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getPath()).isEqualTo("/api/v1/ai/task-duration");
        assertThat(recorded.getMethod()).isEqualTo("POST");
        String sent = recorded.getBody().readUtf8();
        assertThat(sent).contains("\"complexity\":\"HARD\"");
        assertThat(sent).contains("\"energyRequired\":\"HIGH\"");
        assertThat(sent).contains("\"userEstimateMinutes\":60");
        assertThat(sent).doesNotContain("userId");
    }

    @Test
    void returnsEmptyWhenAiIsDown() {
        server.enqueue(new MockResponse().setResponseCode(503).setBody("{\"error\":\"unavailable\"}"));
        assertThat(client.estimateDuration(new TaskDurationAiRequest(null, "EASY", null, 30, null)))
                .isEmpty();
    }

    @Test
    void returnsEmptyOnInvalidJson() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("not-json"));
        assertThat(client.estimateDuration(new TaskDurationAiRequest(null, null, null, 30, null)))
                .isEmpty();
    }
}
