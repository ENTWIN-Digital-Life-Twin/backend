package com.digitallifetwin.ai.dto.request;

/**
 * Mirrors {@code com.digitallifetwin.wellness.client.RecommendationAiRequest} field-for-field.
 */
public record RecommendationRequest(
        Double sleepMinutes,
        Double hydrationMl,
        Double stressLevel,
        Double fatigueLevel,
        Double weeklyWorkoutMinutes,
        Double moodLevel,
        Double dailySteps
) {
}
