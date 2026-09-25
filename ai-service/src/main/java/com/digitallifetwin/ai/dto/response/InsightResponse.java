package com.digitallifetwin.ai.dto.response;

import java.util.List;

public record InsightResponse(
        String id,
        String category,
        String risk,
        int confidence,
        String title,
        String explanation,
        String recommendation,
        List<InsightFactorResponse> factors
) {
}
