package com.digitallifetwin.planning.dto.response;

public record DashboardStatsResponse(
        Integer productivityPercent,
        Integer tasksCompleted,
        Integer tasksTotal,
        String focusTime,
        Integer breaksTaken,
        Integer goalsMetPercent,
        Integer aiConfidence,
        String freeTimeTotal,
        String freeTimeEvening,
        String freeTimeLunch
) {
}
