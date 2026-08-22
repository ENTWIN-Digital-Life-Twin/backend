package com.digitallifetwin.planning.dto.response;

public record DailyPlanSummaryResponse(
        int totalTasks,
        int completedTasks,
        int plannedMinutes,
        int eventMinutes,
        int occupiedMinutes,
        int freeMinutes,
        int conflictCount,
        boolean overloaded
) {
}
