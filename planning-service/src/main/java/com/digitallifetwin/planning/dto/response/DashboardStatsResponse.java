package com.digitallifetwin.planning.dto.response;

public record DashboardStatsResponse(
        Integer productivityPercent,
        Integer productivityChangePercent,
        Integer tasksCompleted,
        Integer tasksTotal,
        String focusTime,
        Integer focusMinutes,
        Integer occupiedMinutes,
        Integer freeMinutes,
        Integer breaksTaken,
        Integer goalsMetPercent,
        Integer priorityGoalsMetPercent,
        Integer aiConfidence,
        String freeTimeTotal,
        String freeTimeEvening,
        String freeTimeLunch,
        Boolean overloaded
) {
}
