package com.digitallifetwin.planning.dto.response;

public record DashboardStatsResponse(
        int productivityPercent,
        int productivityChangePercent,
        int tasksCompleted,
        int tasksTotal,
        int focusMinutes,
        int occupiedMinutes,
        int freeMinutes,
        int priorityGoalsMetPercent,
        boolean overloaded
) {
}
