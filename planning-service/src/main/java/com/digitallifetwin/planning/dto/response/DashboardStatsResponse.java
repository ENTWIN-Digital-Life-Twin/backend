package com.digitallifetwin.planning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private Integer productivityPercent;
    private Integer tasksCompleted;
    private Integer tasksTotal;
    private String focusTime;
    private Integer breaksTaken;
    private Integer goalsMetPercent;
    private Integer aiConfidence;
    private String freeTimeTotal;
    private String freeTimeEvening;
    private String freeTimeLunch;
}
