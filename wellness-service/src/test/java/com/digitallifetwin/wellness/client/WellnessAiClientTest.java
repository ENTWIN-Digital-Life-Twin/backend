package com.digitallifetwin.wellness.client;

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

class WellnessAiClientTest {

    private MockWebServer server;
    private WellnessAiClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        RestClient restClient = RestClient.builder()
                .baseUrl(server.url("/").toString().replaceAll("/$", ""))
                .build();
        client = new WellnessAiClient(restClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void mapsLifestyleRiskCamelCaseAndOmitsUserId() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"riskLevel":"MODERATE","score":55.0,"engine":"RULE_BASED_BASELINE","factors":["LOW_SLEEP"],"modelVersion":null}
                        """));

        Optional<LifestyleRiskAiResponse> result = client.lifestyleRisk(new LifestyleRiskAiRequest(
                420.0, 1700.0, 180.0, 6.2, 5.4, 6.8, 6200.0));

        assertThat(result).isPresent();
        assertThat(result.get().riskLevel()).isEqualTo("MODERATE");
        assertThat(result.get().engine()).isEqualTo("RULE_BASED_BASELINE");
        assertThat(result.get().factors()).containsExactly("LOW_SLEEP");

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getPath()).isEqualTo("/api/v1/ai/lifestyle-risk");
        String sent = recorded.getBody().readUtf8();
        assertThat(sent).contains("\"averageSleepMinutes\":420.0");
        assertThat(sent).contains("\"averageHydrationMl\":1700.0");
        assertThat(sent).doesNotContain("userId");
    }

    @Test
    void mapsRecommendationMessages() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"recommendations":[{"type":"REST","priority":"HIGH","message":"Consider a longer sleep window."}],"engine":"RULE_BASED_BASELINE"}
                        """));

        Optional<RecommendationAiResponse> result = client.recommendations(
                new RecommendationAiRequest(330.0, 900.0, 8.0, 7.0, 20.0, 4.0, 3000.0));

        assertThat(result).isPresent();
        assertThat(result.get().recommendations()).hasSize(1);
        assertThat(result.get().recommendations().getFirst().type()).isEqualTo("REST");
        assertThat(result.get().recommendations().getFirst().message()).contains("sleep");

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getPath()).isEqualTo("/api/v1/ai/recommendations");
        String sent = recorded.getBody().readUtf8();
        assertThat(sent).contains("\"sleepMinutes\":330.0");
        assertThat(sent).contains("\"hydrationMl\":900.0");
    }

    @Test
    void failOpenWhenLifestyleAiReturns503() {
        server.enqueue(new MockResponse().setResponseCode(503));
        assertThat(client.lifestyleRisk(new LifestyleRiskAiRequest(300.0, null, null, null, null, null, null)))
                .isEmpty();
    }

    @Test
    void failOpenWhenRecommendationsAiReturns422() {
        server.enqueue(new MockResponse().setResponseCode(422).setBody("{\"status\":422}"));
        assertThat(client.recommendations(new RecommendationAiRequest(null, null, null, null, null, null, null)))
                .isEmpty();
    }
}
