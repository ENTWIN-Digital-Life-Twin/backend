package com.digitallifetwin.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Mirrors wellness-service {@code WeeklyWellnessSummaryResponse} — only fields ai-service needs. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WellnessWeeklySummary(
        Double averageSleepMinutes,
        Double averageSleepQuality,
        Double averageHydrationMl,
        Integer totalWorkoutMinutes,
        Integer workoutCount,
        Double averageMood,
        Double averageStress,
        Double averageFatigue,
        Double averageDailySteps,
        Double totalCaloriesConsumed
) {
}
