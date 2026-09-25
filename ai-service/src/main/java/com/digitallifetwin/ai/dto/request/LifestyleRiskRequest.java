package com.digitallifetwin.ai.dto.request;

/**
 * Mirrors {@code com.digitallifetwin.wellness.client.LifestyleRiskAiRequest} field-for-field —
 * wellness-service posts this shape without an Authorization header (internal call).
 */
public record LifestyleRiskRequest(
        Double averageSleepMinutes,
        Double averageHydrationMl,
        Double weeklyWorkoutMinutes,
        Double averageStress,
        Double averageFatigue,
        Double averageMood,
        Double averageDailySteps
) {
}
