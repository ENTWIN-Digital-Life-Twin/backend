package com.digitallifetwin.wellness.client;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class WellnessAiClient {

    private final RestClient wellnessAiRestClient;

    public Optional<LifestyleRiskAiResponse> lifestyleRisk(LifestyleRiskAiRequest request) {
        return post("/api/v1/ai/lifestyle-risk", request, LifestyleRiskAiResponse.class);
    }

    public Optional<RecommendationAiResponse> recommendations(RecommendationAiRequest request) {
        return post("/api/v1/ai/recommendations", request, RecommendationAiResponse.class);
    }

    private <T> Optional<T> post(String path, Object body, Class<T> type) {
        try {
            T response = wellnessAiRestClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(type);
            return Optional.ofNullable(response);
        } catch (RestClientException ex) {
            log.warn("ai_unavailable endpoint={} reason={}", path, ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}
