package com.digitallifetwin.wellness.service;

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

    @Transactional(readOnly = true)
    public DashboardWellnessResponse getDashboardMetrics(UUID userId) {
        DailyWellnessSummaryResponse summary = wellnessSummaryService.daily(userId, LocalDate.now());

        return DashboardWellnessResponse.builder()
                .sleep(metric(formatMinutes(summary.sleep().totalMinutes()), percentage(summary.sleep().totalMinutes(), SLEEP_TARGET_MINUTES)))
                .hydration(metric(formatLiters(summary.hydration().totalMl()), clamp(summary.hydration().goalPercentage())))
                .activity(metric(summary.activity().activeMinutes() + " min", percentage(summary.activity().activeMinutes(), ACTIVITY_TARGET_MINUTES)))
                .nutrition(metric(summary.nutrition().mealCount() + " meals", percentage(summary.nutrition().mealCount(), MEAL_TARGET)))
                .mood(metric(formatMood(summary.wellbeing().averageMood()), moodPercentage(summary.wellbeing().averageMood())))
                .build();
    }

    @Transactional(readOnly = true)
    public WeeklyWellnessResponse getWeeklyMetrics(UUID userId) {
        WeeklyWellnessSummaryResponse summary = wellnessSummaryService.weekly(userId, LocalDate.now().minusDays(6));
        List<DailyWellnessSummaryResponse> days = summary.days();

        return WeeklyWellnessResponse.builder()
                .labels(days.stream().map(day -> day.date().getDayOfWeek().name().substring(0, 3)).toList())
                .sleep(days.stream().map(day -> percentage(day.sleep().totalMinutes(), SLEEP_TARGET_MINUTES)).toList())
                .activity(days.stream().map(day -> percentage(day.activity().activeMinutes(), ACTIVITY_TARGET_MINUTES)).toList())
                .nutrition(days.stream().map(day -> percentage(day.nutrition().mealCount(), MEAL_TARGET)).toList())
                .build();
    }

    private DashboardWellnessResponse.MetricData metric(String value, int level) {
        return DashboardWellnessResponse.MetricData.builder().value(value).level(level).build();
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
