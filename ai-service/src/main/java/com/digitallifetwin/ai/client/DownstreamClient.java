package com.digitallifetwin.ai.client;

import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Forwards the caller's bearer token to planning-service / wellness-service so insights and
 * chat replies reflect the authenticated user's own data. Fails open (returns empty) so a
 * downstream outage degrades gracefully instead of breaking the whole insights/chat response.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DownstreamClient {

    private final RestClient planningRestClient;
    private final RestClient wellnessRestClient;

    public Optional<PlanningDashboardStats> planningDashboardStats(String rawToken) {
        return get(planningRestClient, "/api/v1/dashboard/stats", rawToken, PlanningDashboardStats.class);
    }

    public Optional<WellnessWeeklySummary> wellnessWeeklySummary(String rawToken, LocalDate startDate) {
        return get(
                wellnessRestClient,
                "/api/v1/wellness/summary/weekly?startDate=" + startDate,
                rawToken,
                WellnessWeeklySummary.class);
    }

    private <T> Optional<T> get(RestClient client, String path, String rawToken, Class<T> type) {
        try {
            T response = client.get()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + rawToken)
                    .retrieve()
                    .body(type);
            return Optional.ofNullable(response);
        } catch (RestClientException ex) {
            log.warn("downstream_unavailable path={} reason={}", path, ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}
