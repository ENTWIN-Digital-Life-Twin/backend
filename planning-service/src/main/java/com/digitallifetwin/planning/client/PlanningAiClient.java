package com.digitallifetwin.planning.client;

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
public class PlanningAiClient {

    private final RestClient planningAiRestClient;

    public Optional<TaskDurationAiResponse> estimateDuration(TaskDurationAiRequest request) {
        try {
            TaskDurationAiResponse body = planningAiRestClient.post()
                    .uri("/api/v1/ai/task-duration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(TaskDurationAiResponse.class);
            return Optional.ofNullable(body);
        } catch (RestClientException ex) {
            log.warn("ai_unavailable endpoint=task-duration reason={}", ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}
