package com.digitallifetwin.wellness.service;

import com.digitallifetwin.wellness.client.LifestyleRiskAiRequest;
import com.digitallifetwin.wellness.client.LifestyleRiskAiResponse;
import com.digitallifetwin.wellness.client.RecommendationAiRequest;
import com.digitallifetwin.wellness.client.RecommendationAiResponse;
import com.digitallifetwin.wellness.client.WellnessAiClient;
import com.digitallifetwin.wellness.config.WellnessProperties;
import com.digitallifetwin.wellness.dto.response.DailyWellnessSummaryResponse;
import com.digitallifetwin.wellness.dto.response.DashboardWellnessResponse;
import com.digitallifetwin.wellness.dto.response.WeeklyWellnessResponse;
import com.digitallifetwin.wellness.dto.response.WeeklyWellnessSummaryResponse;
import com.digitallifetwin.wellness.service.summary.WellnessSummaryService;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int SLEEP_TARGET_MINUTES = 8 * 60;
    private static final int ACTIVITY_TARGET_MINUTES = 60;
    private static final int MEAL_TARGET = 3;

    private final WellnessSummaryService wellnessSummaryService;
    private final WellnessProperties wellnessProperties;
    private final WellnessAiClient wellnessAiClient;

    @Transactional(readOnly = true)
    public DashboardWellnessResponse getDashboardMetrics(UUID userId) {
        LocalDate today = LocalDate.now(wellnessProperties.zoneId());
        DailyWellnessSummaryResponse summary = wellnessSummaryService.daily(userId, today);
        WeeklyWellnessSummaryResponse weekly = wellnessSummaryService.weekly(userId, today.minusDays(6));

        return new DashboardWellnessResponse(
                metric(formatMinutes(summary.sleep().totalMinutes()), percentage(summary.sleep().totalMinutes(), SLEEP_TARGET_MINUTES)),
                metric(formatLiters(summary.hydration().totalMl()), clamp(summary.hydration().goalPercentage())),
                metric(summary.activity().activeMinutes() + " min", percentage(summary.activity().activeMinutes(), ACTIVITY_TARGET_MINUTES)),
                metric(summary.nutrition().mealCount() + " meals", percentage(summary.nutrition().mealCount(), MEAL_TARGET)),
                metric(formatMood(summary.wellbeing().averageMood()), moodPercentage(summary.wellbeing().averageMood())),
                lifestyleRisk(weekly),
                recommendationMessages(weekly)
        );
    }

    @Transactional(readOnly = true)
    public WeeklyWellnessResponse getWeeklyMetrics(UUID userId) {
        LocalDate start = LocalDate.now(wellnessProperties.zoneId()).minusDays(6);
        WeeklyWellnessSummaryResponse summary = wellnessSummaryService.weekly(userId, start);
        List<DailyWellnessSummaryResponse> days = summary.days();
        return new WeeklyWellnessResponse(
                days.stream().map(day -> day.date().getDayOfWeek().name().substring(0, 3)).toList(),
                days.stream().map(day -> percentage(day.sleep().totalMinutes(), SLEEP_TARGET_MINUTES)).toList(),
                days.stream().map(day -> percentage(day.activity().activeMinutes(), ACTIVITY_TARGET_MINUTES)).toList(),
                days.stream().map(day -> percentage(day.nutrition().mealCount(), MEAL_TARGET)).toList()
        );
    }

    private String lifestyleRisk(WeeklyWellnessSummaryResponse weekly) {
        return wellnessAiClient.lifestyleRisk(toLifestyleRequest(weekly))
                .map(LifestyleRiskAiResponse::riskLevel)
                .orElse(null);
    }

    private List<String> recommendationMessages(WeeklyWellnessSummaryResponse weekly) {
        return wellnessAiClient.recommendations(toRecommendationRequest(weekly))
                .map(RecommendationAiResponse::recommendations)
                .orElse(List.of())
                .stream()
                .map(RecommendationAiResponse.RecommendationAiItem::message)
                .toList();
    }

    private LifestyleRiskAiRequest toLifestyleRequest(WeeklyWellnessSummaryResponse weekly) {
        return new LifestyleRiskAiRequest(
                weekly.averageSleepMinutes(),
                weekly.averageHydrationMl(),
                weekly.totalWorkoutMinutes() == null ? null : weekly.totalWorkoutMinutes().doubleValue(),
                weekly.averageStress(),
                weekly.averageFatigue(),
                weekly.averageMood(),
                weekly.averageDailySteps()
        );
    }

    private RecommendationAiRequest toRecommendationRequest(WeeklyWellnessSummaryResponse weekly) {
        return new RecommendationAiRequest(
                weekly.averageSleepMinutes(),
                weekly.averageHydrationMl(),
                weekly.averageStress(),
                weekly.averageFatigue(),
                weekly.totalWorkoutMinutes() == null ? null : weekly.totalWorkoutMinutes().doubleValue(),
                weekly.averageMood(),
                weekly.averageDailySteps()
        );
    }

    private DashboardWellnessResponse.MetricData metric(String value, int level) {
        return new DashboardWellnessResponse.MetricData(value, level);
    }

    private String formatMinutes(Integer totalMinutes) {
        if (totalMinutes == null || totalMinutes == 0) {
            return "--";
        }
        return totalMinutes / 60 + "h " + totalMinutes % 60 + "m";
    }

    private String formatLiters(int totalMl) {
        if (totalMl == 0) {
            return "--";
        }
        return String.format(Locale.ROOT, "%.1f L", totalMl / 1000.0);
    }

    private String formatMood(Double mood) {
        return mood == null ? "--" : String.format(Locale.ROOT, "%.1f/10", mood);
    }

    private int moodPercentage(Double mood) {
        return mood == null ? 0 : clamp(mood * 10);
    }

    private int percentage(Number value, int target) {
        return value == null || target <= 0 ? 0 : clamp(value.doubleValue() * 100 / target);
    }

    private int clamp(Double value) {
        return value == null ? 0 : (int) Math.round(Math.max(0, Math.min(100, value)));
    }
}
