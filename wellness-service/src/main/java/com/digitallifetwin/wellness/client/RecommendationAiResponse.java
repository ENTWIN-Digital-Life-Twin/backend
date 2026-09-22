package com.digitallifetwin.wellness.client;

import java.util.List;

public record RecommendationAiResponse(
        List<RecommendationAiItem> recommendations,
        String engine
) {

    public record RecommendationAiItem(String type, String priority, String message) {
    }
}
