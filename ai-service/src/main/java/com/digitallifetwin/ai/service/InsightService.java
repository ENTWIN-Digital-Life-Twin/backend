package com.digitallifetwin.ai.service;

import com.digitallifetwin.ai.client.DownstreamClient;
import com.digitallifetwin.ai.client.PlanningDashboardStats;
import com.digitallifetwin.ai.client.WellnessWeeklySummary;
import com.digitallifetwin.ai.dto.response.InsightFactorResponse;
import com.digitallifetwin.ai.dto.response.InsightResponse;
import com.digitallifetwin.ai.dto.response.InsightsSummaryResponse;
import com.digitallifetwin.ai.dto.response.RecommendationResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Aggregates live planning-service and wellness-service data for the authenticated user and
 * runs it through {@link RuleEngineService} to build the "AI insights" feed. No external LLM —
 * this is a transparent, rule-based baseline that degrades gracefully if a downstream service
 * is unreachable.
 */
@Service
@RequiredArgsConstructor
public class InsightService {

    private final DownstreamClient downstreamClient;
    private final RuleEngineService ruleEngine;

    public InsightsSummaryResponse buildInsights(String rawToken) {
        Optional<PlanningDashboardStats> planning = downstreamClient.planningDashboardStats(rawToken);
        Optional<WellnessWeeklySummary> wellness = downstreamClient.wellnessWeeklySummary(
                rawToken, LocalDate.now().minusDays(6));

        List<InsightResponse> insights = new ArrayList<>();
        wellness.ifPresent(summary -> insights.add(wellnessInsight(summary)));
        planning.ifPresent(stats -> insights.add(productivityInsight(stats)));
        planning.ifPresent(stats -> insights.add(tasksInsight(stats)));
        planning.ifPresent(stats -> insights.add(scheduleInsight(stats)));
        wellness.filter(summary -> summary.totalCaloriesConsumed() != null)
                .ifPresent(summary -> insights.add(nutritionInsight(summary)));

        RuleEngineService.RiskAssessment overallRisk = wellness
                .map(this::toEngineInputs)
                .map(ruleEngine::assessRisk)
                .orElse(new RuleEngineService.RiskAssessment("LOW", 0, List.of()));

        int globalScore = planning
                .map(stats -> (int) Math.round((stats.productivityPercent() + (100 - overallRisk.score())) / 2.0))
                .orElse((int) Math.round(100 - overallRisk.score()));

        return new InsightsSummaryResponse(
                Math.max(0, Math.min(100, globalScore)),
                overallRisk.riskLevel(),
                insights);
    }

    private RuleEngineService.Inputs toEngineInputs(WellnessWeeklySummary summary) {
        Double weeklyWorkoutMinutes = summary.totalWorkoutMinutes() == null
                ? null : summary.totalWorkoutMinutes().doubleValue();
        return new RuleEngineService.Inputs(
                summary.averageSleepMinutes(),
                summary.averageHydrationMl(),
                weeklyWorkoutMinutes,
                summary.averageStress(),
                summary.averageFatigue(),
                summary.averageMood(),
                summary.averageDailySteps());
    }

    private InsightResponse wellnessInsight(WellnessWeeklySummary summary) {
        RuleEngineService.Inputs inputs = toEngineInputs(summary);
        RuleEngineService.RiskAssessment risk = ruleEngine.assessRisk(inputs);
        RecommendationResponse recommendation = ruleEngine.recommend(inputs);
        String message = recommendation.recommendations().isEmpty()
                ? "Keep up the good habits."
                : recommendation.recommendations().get(0).message();

        List<InsightFactorResponse> factors = new ArrayList<>();
        if (summary.averageSleepMinutes() != null) {
            factors.add(new InsightFactorResponse("Sleep", formatMinutes(summary.averageSleepMinutes())));
        }
        if (summary.averageHydrationMl() != null) {
            factors.add(new InsightFactorResponse("Hydration", formatLiters(summary.averageHydrationMl())));
        }
        if (summary.averageMood() != null) {
            factors.add(new InsightFactorResponse("Mood", String.format(Locale.ROOT, "%.1f/10", summary.averageMood())));
        }
        if (summary.averageStress() != null) {
            factors.add(new InsightFactorResponse("Stress", String.format(Locale.ROOT, "%.1f/10", summary.averageStress())));
        }

        return new InsightResponse(
                "wellness-weekly",
                "wellness",
                risk.riskLevel().toLowerCase(Locale.ROOT),
                confidenceFor(risk),
                "Weekly wellness balance",
                "Based on this week's sleep, hydration, mood and stress data.",
                message,
                factors);
    }

    private InsightResponse productivityInsight(PlanningDashboardStats stats) {
        String risk = stats.overloaded() ? "moderate" : stats.productivityPercent() < 40 ? "moderate" : "low";
        String recommendation = stats.overloaded()
                ? "Your schedule looks overloaded today — consider moving a lower-priority task to tomorrow."
                : stats.productivityChangePercent() < 0
                        ? "Productivity dipped versus your recent average. A short planning session could help re-focus."
                        : "Productivity is on track — keep blocking focus time for your top priority.";

        return new InsightResponse(
                "productivity-today",
                "productivity",
                risk,
                82,
                "Productivity today",
                "Derived from today's completed tasks and focus time versus your plan.",
                recommendation,
                List.of(
                        new InsightFactorResponse("Productivity", stats.productivityPercent() + "%"),
                        new InsightFactorResponse("Focus time", stats.focusMinutes() + " min")));
    }

    private InsightResponse tasksInsight(PlanningDashboardStats stats) {
        double ratio = stats.tasksTotal() == 0 ? 1.0 : (double) stats.tasksCompleted() / stats.tasksTotal();
        String risk = ratio < 0.4 ? "moderate" : "low";
        String recommendation = ratio >= 1.0
                ? "All planned tasks are complete — nice work today."
                : "You still have tasks pending today. Tackling the highest priority one first usually pays off.";

        return new InsightResponse(
                "tasks-today",
                "tasks",
                risk,
                80,
                "Tasks completion",
                "Tasks completed versus tasks planned for today.",
                recommendation,
                List.of(new InsightFactorResponse(
                        "Completed", stats.tasksCompleted() + " / " + stats.tasksTotal())));
    }

    private InsightResponse scheduleInsight(PlanningDashboardStats stats) {
        String risk = stats.overloaded() ? "high" : "low";
        String recommendation = stats.overloaded()
                ? "Your day is fully booked with little free time. Leave a buffer between meetings if you can."
                : "You have breathing room in today's schedule for deep-focus work.";

        return new InsightResponse(
                "schedule-today",
                "schedule",
                risk,
                86,
                "Schedule load",
                "Occupied versus free time in today's plan.",
                recommendation,
                List.of(
                        new InsightFactorResponse("Occupied", stats.occupiedMinutes() + " min"),
                        new InsightFactorResponse("Free", stats.freeMinutes() + " min")));
    }

    private InsightResponse nutritionInsight(WellnessWeeklySummary summary) {
        double avgCalories = summary.totalCaloriesConsumed() / 7.0;
        String risk = avgCalories < 1200 || avgCalories > 2800 ? "moderate" : "low";
        String recommendation = risk.equals("moderate")
                ? "Your average daily calorie intake this week looks outside a typical range — worth reviewing your meals."
                : "Calorie intake this week looks balanced — keep favouring whole foods.";

        return new InsightResponse(
                "nutrition-weekly",
                "nutrition",
                risk,
                80,
                "Nutrition this week",
                "Average daily calories logged this week.",
                recommendation,
                List.of(new InsightFactorResponse("Avg. calories/day", Math.round(avgCalories) + " kcal")));
    }

    private int confidenceFor(RuleEngineService.RiskAssessment risk) {
        return switch (risk.riskLevel()) {
            case "HIGH" -> 88;
            case "MODERATE" -> 82;
            default -> 80;
        };
    }

    private String formatMinutes(double minutes) {
        int h = (int) (minutes / 60);
        int m = (int) Math.round(minutes % 60);
        return h + "h " + m + "m";
    }

    private String formatLiters(double ml) {
        return String.format(Locale.ROOT, "%.1f L", ml / 1000.0);
    }
}
