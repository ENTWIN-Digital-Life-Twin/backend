package com.digitallifetwin.wellness.dto.response;

import java.time.LocalDate;
import java.util.List;

public record WeeklyWellnessSummaryResponse(
        LocalDate startDate,
        LocalDate endDate,
        String timezone,
        Double averageSleepMinutes,
        Double averageSleepQuality,
        Double averageHydrationMl,
        Integer totalWorkoutMinutes,
        Integer workoutCount,
        Double averageMood,
        Double averageStress,
        Double averageFatigue,
        Double averageDailySteps,
        Double totalCaloriesConsumed,
        List<DailyWellnessSummaryResponse> days
) {
}
