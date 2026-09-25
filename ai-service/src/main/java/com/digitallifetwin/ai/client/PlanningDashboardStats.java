package com.digitallifetwin.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Mirrors planning-service {@code DashboardStatsResponse} — only fields ai-service needs. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlanningDashboardStats(
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
