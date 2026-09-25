package com.digitallifetwin.ai.dto.response;

import java.util.List;

/**
 * Mirrors {@code com.digitallifetwin.wellness.client.RecommendationAiResponse} field-for-field.
 */
public record RecommendationResponse(
        List<RecommendationItem> recommendations,
        String engine
) {

    public record RecommendationItem(String type, String priority, String message) {
    }
}
