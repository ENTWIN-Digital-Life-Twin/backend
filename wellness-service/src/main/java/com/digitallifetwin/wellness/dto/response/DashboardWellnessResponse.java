package com.digitallifetwin.wellness.dto.response;

public record DashboardWellnessResponse(
        MetricData sleep,
        MetricData hydration,
        MetricData activity,
        MetricData nutrition,
        MetricData mood,
        String riskLevel,
        java.util.List<String> recommendations
) {

    public record MetricData(String value, Integer level) {
    }
}
