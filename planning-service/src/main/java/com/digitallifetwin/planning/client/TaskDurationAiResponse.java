package com.digitallifetwin.planning.client;

public record TaskDurationAiResponse(
        Integer predictedDurationMinutes,
        String engine,
        Double confidence
) {
}
