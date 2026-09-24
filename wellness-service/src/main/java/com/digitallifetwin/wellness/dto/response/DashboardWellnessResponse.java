package com.digitallifetwin.wellness.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardWellnessResponse {
    private MetricData sleep;
    private MetricData hydration;
    private MetricData activity;
    private MetricData nutrition;
    private MetricData mood;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricData {
        private String value;  // Formatted value (e.g., "7h 20m", "1.7 L")
        private Integer level; // Percentage 0-100
    }
}
