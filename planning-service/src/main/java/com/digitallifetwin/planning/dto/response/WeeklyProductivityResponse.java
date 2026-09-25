package com.digitallifetwin.planning.dto.response;

import java.util.List;

public record WeeklyProductivityResponse(
        List<String> labels,
        List<Integer> productivity,
        List<Integer> tasksCompleted,
        List<Integer> tasksTotal,
        List<Integer> focusMinutes
) {
}
