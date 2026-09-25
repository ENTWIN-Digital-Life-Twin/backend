package com.digitallifetwin.ai.service;

import com.digitallifetwin.ai.dto.response.RecommendationResponse;
import com.digitallifetwin.ai.dto.response.RecommendationResponse.RecommendationItem;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Deterministic, explainable "rule-based baseline" engine — no external LLM call, no API key
 * required. Every threshold below is a simple, documented heuristic so the risk score and
 * recommendations are reproducible and testable.
 */
@Service
public class RuleEngineService {

    public static final String ENGINE_NAME = "RULE_BASED_BASELINE";
    public static final String MODEL_VERSION = "v1";

    public static final int SLEEP_TARGET_MINUTES = 480;
    public static final int LOW_SLEEP_MINUTES = 420;
    public static final int HYDRATION_TARGET_ML = 2500;
    public static final int LOW_HYDRATION_ML = 1500;
    public static final int WEEKLY_ACTIVITY_TARGET_MINUTES = 150;
    public static final int LOW_WEEKLY_ACTIVITY_MINUTES = 90;
    public static final double HIGH_STRESS_LEVEL = 7.0;
    public static final double HIGH_FATIGUE_LEVEL = 7.0;
    public static final double LOW_MOOD_LEVEL = 4.0;
    public static final int STEPS_TARGET = 8000;
    public static final int LOW_STEPS = 5000;

    public record Factor(String code, int weight) {
    }

    public record RiskAssessment(String riskLevel, double score, List<String> factors) {
    }

    public record Inputs(
            Double sleepMinutes,
            Double hydrationMl,
            Double weeklyWorkoutMinutes,
            Double stress,
            Double fatigue,
            Double mood,
            Double dailySteps
    ) {
    }

    public RiskAssessment assessRisk(Inputs inputs) {
        List<String> factors = new ArrayList<>();
        double score = 0;

        if (inputs.sleepMinutes() != null && inputs.sleepMinutes() < LOW_SLEEP_MINUTES) {
            factors.add("LOW_SLEEP");
            score += 20;
        }
        if (inputs.hydrationMl() != null && inputs.hydrationMl() < LOW_HYDRATION_ML) {
            factors.add("LOW_HYDRATION");
            score += 15;
        }
        if (inputs.weeklyWorkoutMinutes() != null && inputs.weeklyWorkoutMinutes() < LOW_WEEKLY_ACTIVITY_MINUTES) {
            factors.add("LOW_ACTIVITY");
            score += 15;
        }
        if (inputs.stress() != null && inputs.stress() >= HIGH_STRESS_LEVEL) {
            factors.add("HIGH_STRESS");
            score += 20;
        }
        if (inputs.fatigue() != null && inputs.fatigue() >= HIGH_FATIGUE_LEVEL) {
            factors.add("HIGH_FATIGUE");
            score += 15;
        }
        if (inputs.mood() != null && inputs.mood() <= LOW_MOOD_LEVEL) {
            factors.add("LOW_MOOD");
            score += 10;
        }
        if (inputs.dailySteps() != null && inputs.dailySteps() < LOW_STEPS) {
            factors.add("LOW_STEPS");
            score += 10;
        }

        double clamped = Math.max(0, Math.min(100, score));
        String level = clamped < 30 ? "LOW" : clamped < 60 ? "MODERATE" : "HIGH";
        return new RiskAssessment(level, clamped, factors);
    }

    public RecommendationResponse recommend(Inputs inputs) {
        RiskAssessment risk = assessRisk(inputs);
        List<RecommendationItem> items = new ArrayList<>();

        for (String factor : risk.factors()) {
            items.add(switch (factor) {
                case "LOW_SLEEP" -> new RecommendationItem(
                        "SLEEP", "HIGH",
                        "You're averaging under 7h of sleep. Try moving bedtime 30-45 minutes earlier to close the gap toward your 8h goal.");
                case "LOW_HYDRATION" -> new RecommendationItem(
                        "HYDRATION", "MEDIUM",
                        "Hydration is below target. Keep a bottle within reach and aim for a glass of water every 2 hours.");
                case "LOW_ACTIVITY" -> new RecommendationItem(
                        "ACTIVITY", "MEDIUM",
                        "Weekly activity is under the 150-minute guideline. A brisk 20-minute walk most days would help close the gap.");
                case "HIGH_STRESS" -> new RecommendationItem(
                        "REST", "HIGH",
                        "Stress has been trending high. Consider a short breathing break or a longer sleep window tonight.");
                case "HIGH_FATIGUE" -> new RecommendationItem(
                        "REST", "MEDIUM",
                        "Fatigue is elevated — prioritise recovery today and avoid scheduling high-intensity workouts.");
                case "LOW_MOOD" -> new RecommendationItem(
                        "WELLBEING", "MEDIUM",
                        "Mood has been on the lower side. A short walk outside or connecting with a friend can help reset it.");
                case "LOW_STEPS" -> new RecommendationItem(
                        "ACTIVITY", "LOW",
                        "Daily steps are below target. Try taking calls standing up or a 10-minute walk after meals.");
                default -> new RecommendationItem("GENERAL", "LOW", "Keep up the good habits.");
            });
        }

        if (items.isEmpty()) {
            items.add(new RecommendationItem(
                    "GENERAL", "LOW", "Great balance this week across sleep, activity and mood — keep it up!"));
        }

        return new RecommendationResponse(items, ENGINE_NAME);
    }
}
