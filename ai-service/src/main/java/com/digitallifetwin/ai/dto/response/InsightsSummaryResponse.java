package com.digitallifetwin.ai.dto.response;

import java.util.List;

public record InsightsSummaryResponse(
        int globalScore,
        String riskLevel,
        List<InsightResponse> insights
) {
}
