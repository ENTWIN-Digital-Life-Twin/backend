package com.digitallifetwin.ai.service;

import com.digitallifetwin.ai.client.DownstreamClient;
import com.digitallifetwin.ai.client.PlanningDashboardStats;
import com.digitallifetwin.ai.client.WellnessWeeklySummary;
import com.digitallifetwin.ai.dto.response.ChatResponse;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Keyword-driven assistant reply, backed by the caller's own live planning/wellness data.
 * Intentionally not an LLM integration (no external API key in this environment) — replies are
 * templated but reference real numbers fetched on behalf of the authenticated user.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final DownstreamClient downstreamClient;
    private final RuleEngineService ruleEngine;

    public ChatResponse reply(String rawToken, String message) {
        String text = message.toLowerCase(Locale.ROOT);

        if (containsAny(text, "sleep", "sommeil", "نوم")) {
            return sleepReply(rawToken);
        }
        if (containsAny(text, "hydrat", "water", "eau", "ماء")) {
            return hydrationReply(rawToken);
        }
        if (containsAny(text, "task", "tâche", "tache", "todo", "مهمة", "مهام")) {
            return tasksReply(rawToken);
        }
        if (containsAny(text, "productiv", "focus")) {
            return productivityReply(rawToken);
        }
        if (containsAny(text, "stress", "anxi")) {
            return stressReply(rawToken);
        }
        if (containsAny(text, "mood", "humeur", "مزاج")) {
            return moodReply(rawToken);
        }
        if (containsAny(text, "workout", "exercise", "activity", "sport")) {
            return activityReply(rawToken);
        }
        if (containsAny(text, "risk", "balance", "how am i", "overall")) {
            return riskReply(rawToken);
        }

        return new ChatResponse(
                "I can talk about your sleep, hydration, mood, stress, activity, tasks or overall "
                        + "balance — try asking \"How is my sleep this week?\" or \"Am I overloaded today?\".",
                RuleEngineService.ENGINE_NAME);
    }

    private ChatResponse sleepReply(String rawToken) {
        Optional<WellnessWeeklySummary> summary = weekly(rawToken);
        if (summary.isEmpty() || summary.get().averageSleepMinutes() == null) {
            return fallback("I don't have any sleep records for this week yet — log a night's sleep and ask me again.");
        }
        double minutes = summary.get().averageSleepMinutes();
        double hours = minutes / 60.0;
        String verdict = minutes < RuleEngineService.LOW_SLEEP_MINUTES
                ? "That's below the 8h target — try shifting bedtime earlier tonight."
                : "That's a solid average — keep it consistent.";
        return new ChatResponse(
                String.format(Locale.ROOT, "You've averaged %.1fh of sleep this week. %s", hours, verdict),
                RuleEngineService.ENGINE_NAME);
    }

    private ChatResponse hydrationReply(String rawToken) {
        Optional<WellnessWeeklySummary> summary = weekly(rawToken);
        if (summary.isEmpty() || summary.get().averageHydrationMl() == null) {
            return fallback("I don't have hydration entries for this week yet — log a water intake and ask again.");
        }
        double liters = summary.get().averageHydrationMl() / 1000.0;
        String verdict = summary.get().averageHydrationMl() < RuleEngineService.LOW_HYDRATION_ML
                ? "That's under your target — try drinking a glass of water every couple of hours."
                : "You're close to or above target — nice work.";
        return new ChatResponse(
                String.format(Locale.ROOT, "You've averaged %.1f L of water per day this week. %s", liters, verdict),
                RuleEngineService.ENGINE_NAME);
    }

    private ChatResponse tasksReply(String rawToken) {
        Optional<PlanningDashboardStats> stats = planning(rawToken);
        if (stats.isEmpty()) {
            return fallback("I couldn't reach the planning service to check your tasks right now.");
        }
        PlanningDashboardStats s = stats.get();
        return new ChatResponse(
                String.format(Locale.ROOT,
                        "You've completed %d of %d tasks today (%d%% productivity).%s",
                        s.tasksCompleted(), s.tasksTotal(), s.productivityPercent(),
                        s.overloaded() ? " Your schedule looks overloaded — consider deferring something." : ""),
                RuleEngineService.ENGINE_NAME);
    }

    private ChatResponse productivityReply(String rawToken) {
        Optional<PlanningDashboardStats> stats = planning(rawToken);
        if (stats.isEmpty()) {
            return fallback("I couldn't reach the planning service to check your productivity right now.");
        }
        PlanningDashboardStats s = stats.get();
        String trend = s.productivityChangePercent() >= 0
                ? "up " + s.productivityChangePercent() + "% versus your recent average"
                : "down " + Math.abs(s.productivityChangePercent()) + "% versus your recent average";
        return new ChatResponse(
                String.format(Locale.ROOT,
                        "Productivity today is %d%%, %s, with %d minutes of focus time logged.",
                        s.productivityPercent(), trend, s.focusMinutes()),
                RuleEngineService.ENGINE_NAME);
    }

    private ChatResponse stressReply(String rawToken) {
        Optional<WellnessWeeklySummary> summary = weekly(rawToken);
        if (summary.isEmpty() || summary.get().averageStress() == null) {
            return fallback("I don't have mood/stress entries for this week yet.");
        }
        double stress = summary.get().averageStress();
        String verdict = stress >= RuleEngineService.HIGH_STRESS_LEVEL
                ? "That's on the high side — a short breathing break between tasks can help."
                : "That's a manageable level — keep monitoring it.";
        return new ChatResponse(
                String.format(Locale.ROOT, "Your average stress level this week is %.1f/10. %s", stress, verdict),
                RuleEngineService.ENGINE_NAME);
    }

    private ChatResponse moodReply(String rawToken) {
        Optional<WellnessWeeklySummary> summary = weekly(rawToken);
        if (summary.isEmpty() || summary.get().averageMood() == null) {
            return fallback("I don't have mood entries for this week yet.");
        }
        double mood = summary.get().averageMood();
        String verdict = mood <= RuleEngineService.LOW_MOOD_LEVEL
                ? "It's been a bit low — a short walk or catching up with a friend might help lift it."
                : "It's holding up well this week.";
        return new ChatResponse(
                String.format(Locale.ROOT, "Your average mood this week is %.1f/10. %s", mood, verdict),
                RuleEngineService.ENGINE_NAME);
    }

    private ChatResponse activityReply(String rawToken) {
        Optional<WellnessWeeklySummary> summary = weekly(rawToken);
        if (summary.isEmpty() || summary.get().totalWorkoutMinutes() == null) {
            return fallback("I don't have any workouts logged for this week yet.");
        }
        int minutes = summary.get().totalWorkoutMinutes();
        String verdict = minutes < RuleEngineService.LOW_WEEKLY_ACTIVITY_MINUTES
                ? "That's below the 150 min/week guideline — a few brisk walks would help close the gap."
                : "You're close to or above the weekly activity guideline — great consistency.";
        return new ChatResponse(
                String.format(Locale.ROOT, "You've logged %d minutes of activity this week. %s", minutes, verdict),
                RuleEngineService.ENGINE_NAME);
    }

    private ChatResponse riskReply(String rawToken) {
        Optional<WellnessWeeklySummary> summary = weekly(rawToken);
        if (summary.isEmpty()) {
            return fallback("I don't have enough wellness data this week to gauge your overall balance yet.");
        }
        WellnessWeeklySummary s = summary.get();
        RuleEngineService.Inputs inputs = new RuleEngineService.Inputs(
                s.averageSleepMinutes(), s.averageHydrationMl(),
                s.totalWorkoutMinutes() == null ? null : s.totalWorkoutMinutes().doubleValue(),
                s.averageStress(), s.averageFatigue(), s.averageMood(), s.averageDailySteps());
        RuleEngineService.RiskAssessment risk = ruleEngine.assessRisk(inputs);
        String detail = risk.factors().isEmpty()
                ? "no red flags in sleep, hydration, activity, stress or mood."
                : "flags in: " + String.join(", ", risk.factors()).toLowerCase(Locale.ROOT).replace('_', ' ') + ".";
        return new ChatResponse(
                String.format(Locale.ROOT, "Your overall lifestyle risk this week is %s — %s", risk.riskLevel(), detail),
                RuleEngineService.ENGINE_NAME);
    }

    private Optional<WellnessWeeklySummary> weekly(String rawToken) {
        return downstreamClient.wellnessWeeklySummary(rawToken, LocalDate.now().minusDays(6));
    }

    private Optional<PlanningDashboardStats> planning(String rawToken) {
        return downstreamClient.planningDashboardStats(rawToken);
    }

    private ChatResponse fallback(String message) {
        return new ChatResponse(message, RuleEngineService.ENGINE_NAME);
    }

    private boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
