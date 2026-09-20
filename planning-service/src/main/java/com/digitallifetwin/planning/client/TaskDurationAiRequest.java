package com.digitallifetwin.planning.client;

public record TaskDurationAiRequest(
        String category,
        String complexity,
        String energyRequired,
        Integer userEstimateMinutes,
        Integer historicalAverageMinutes
) {
}
