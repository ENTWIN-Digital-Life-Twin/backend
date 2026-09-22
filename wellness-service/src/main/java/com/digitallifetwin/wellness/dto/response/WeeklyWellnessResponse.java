package com.digitallifetwin.wellness.dto.response;

import java.util.List;

public record WeeklyWellnessResponse(
        List<String> labels,
        List<Integer> sleep,
        List<Integer> activity,
        List<Integer> nutrition
) {
}
