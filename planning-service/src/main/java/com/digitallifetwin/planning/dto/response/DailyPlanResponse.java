package com.digitallifetwin.planning.dto.response;

import java.time.LocalDate;
import java.util.List;

public record DailyPlanResponse(
        LocalDate date,
        String timezone,
        List<TaskResponse> tasks,
        List<EventResponse> events,
        DailyPlanSummaryResponse summary
) {
}
