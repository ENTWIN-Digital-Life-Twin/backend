package com.digitallifetwin.wellness.client;

public record LifestyleRiskAiRequest(
        Double averageSleepMinutes,
        Double averageHydrationMl,
        Double weeklyWorkoutMinutes,
        Double averageStress,
        Double averageFatigue,
        Double averageMood,
        Double averageDailySteps
) {
}
