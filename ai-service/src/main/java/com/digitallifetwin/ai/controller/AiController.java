package com.digitallifetwin.ai.controller;

import com.digitallifetwin.ai.dto.request.ChatRequest;
import com.digitallifetwin.ai.dto.request.LifestyleRiskRequest;
import com.digitallifetwin.ai.dto.request.RecommendationRequest;
import com.digitallifetwin.ai.dto.response.ChatResponse;
import com.digitallifetwin.ai.dto.response.InsightsSummaryResponse;
import com.digitallifetwin.ai.dto.response.LifestyleRiskResponse;
import com.digitallifetwin.ai.dto.response.RecommendationResponse;
import com.digitallifetwin.ai.security.SecurityUtils;
import com.digitallifetwin.ai.service.ChatService;
import com.digitallifetwin.ai.service.InsightService;
import com.digitallifetwin.ai.service.RuleEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI")
public class AiController {

    private final RuleEngineService ruleEngine;
    private final InsightService insightService;
    private final ChatService chatService;

    @PostMapping("/lifestyle-risk")
    @Operation(summary = "Rule-based lifestyle risk score (internal call from wellness-service, no auth)")
    public ResponseEntity<LifestyleRiskResponse> lifestyleRisk(@RequestBody LifestyleRiskRequest request) {
        RuleEngineService.RiskAssessment risk = ruleEngine.assessRisk(new RuleEngineService.Inputs(
                request.averageSleepMinutes(),
                request.averageHydrationMl(),
                request.weeklyWorkoutMinutes(),
                request.averageStress(),
                request.averageFatigue(),
                request.averageMood(),
                request.averageDailySteps()));
        return ResponseEntity.ok(new LifestyleRiskResponse(
                risk.riskLevel(), risk.score(), RuleEngineService.ENGINE_NAME, risk.factors(),
                RuleEngineService.MODEL_VERSION));
    }

    @PostMapping("/recommendations")
    @Operation(summary = "Rule-based recommendations (internal call from wellness-service, no auth)")
    public ResponseEntity<RecommendationResponse> recommendations(@RequestBody RecommendationRequest request) {
        RecommendationResponse response = ruleEngine.recommend(new RuleEngineService.Inputs(
                request.sleepMinutes(),
                request.hydrationMl(),
                request.weeklyWorkoutMinutes(),
                request.stressLevel(),
                request.fatigueLevel(),
                request.moodLevel(),
                request.dailySteps()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/insights")
    @Operation(summary = "AI Assistant insights feed for the authenticated user")
    public ResponseEntity<InsightsSummaryResponse> insights() {
        return ResponseEntity.ok(insightService.buildInsights(SecurityUtils.currentRawToken()));
    }

    @PostMapping("/chat")
    @Operation(summary = "Ask the assistant a question about your own lifestyle data")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.reply(SecurityUtils.currentRawToken(), request.question()));
    }
}
