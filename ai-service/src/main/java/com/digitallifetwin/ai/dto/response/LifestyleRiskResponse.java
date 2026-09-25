package com.digitallifetwin.ai.dto.response;

import java.util.List;

/**
 * Mirrors {@code com.digitallifetwin.wellness.client.LifestyleRiskAiResponse} field-for-field.
 */
public record LifestyleRiskResponse(
        String riskLevel,
        Double score,
        String engine,
        List<String> factors,
        String modelVersion
) {
}
