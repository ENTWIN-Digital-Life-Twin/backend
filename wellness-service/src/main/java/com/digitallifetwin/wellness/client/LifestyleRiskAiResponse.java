package com.digitallifetwin.wellness.client;

import java.util.List;

public record LifestyleRiskAiResponse(
        String riskLevel,
        Double score,
        String engine,
        List<String> factors,
        String modelVersion
) {
}
