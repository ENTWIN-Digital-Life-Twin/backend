package com.digitallifetwin.wellness.dto.response;

import java.time.LocalDate;
import java.util.List;

public record DailyWellnessSummaryResponse(
        LocalDate date,
        String timezone,
        SleepSummary sleep,
        HydrationSummary hydration,
        NutritionSummary nutrition,
        ActivitySummary activity,
        WellbeingSummary wellbeing
) {

    public record SleepSummary(Integer totalMinutes, Double hours, Double averageQuality) {
    }

    public record HydrationSummary(int totalMl, int goalMl, Double goalPercentage) {
    }

    public record NutritionSummary(
            int mealCount,
            Double totalCalories,
            Double proteinGrams,
            Double carbohydrateGrams,
            Double fatGrams
    ) {
    }

    public record ActivitySummary(
            int workoutCount,
            int activeMinutes,
            Double caloriesBurned,
            Integer steps
    ) {
    }

    public record WellbeingSummary(
            Double averageMood,
            Double averageStress,
            Double averageFatigue
    ) {
    }
}
