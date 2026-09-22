package com.digitallifetwin.wellness.client;

public record RecommendationAiRequest(
        Double sleepMinutes,
        Double hydrationMl,
        Double stressLevel,
        Double fatigueLevel,
        Double weeklyWorkoutMinutes,
        Double moodLevel,
        Double dailySteps
) {
}
