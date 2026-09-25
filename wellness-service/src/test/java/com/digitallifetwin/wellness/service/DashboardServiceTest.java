package com.digitallifetwin.wellness.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.digitallifetwin.wellness.client.LifestyleRiskAiResponse;
import com.digitallifetwin.wellness.client.RecommendationAiResponse;
import com.digitallifetwin.wellness.client.WellnessAiClient;
import com.digitallifetwin.wellness.config.WellnessProperties;
import com.digitallifetwin.wellness.dto.response.DailyWellnessSummaryResponse;
import com.digitallifetwin.wellness.dto.response.DailyWellnessSummaryResponse.ActivitySummary;
import com.digitallifetwin.wellness.dto.response.DailyWellnessSummaryResponse.HydrationSummary;
import com.digitallifetwin.wellness.dto.response.DailyWellnessSummaryResponse.NutritionSummary;
import com.digitallifetwin.wellness.dto.response.DailyWellnessSummaryResponse.SleepSummary;
import com.digitallifetwin.wellness.dto.response.DailyWellnessSummaryResponse.WellbeingSummary;
import com.digitallifetwin.wellness.dto.response.DashboardWellnessResponse;
import com.digitallifetwin.wellness.dto.response.WeeklyWellnessSummaryResponse;
import com.digitallifetwin.wellness.service.summary.WellnessSummaryService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private WellnessSummaryService wellnessSummaryService;
    @Mock
    private WellnessAiClient wellnessAiClient;

    private DashboardService dashboardService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        dashboardService = new DashboardService(
                wellnessSummaryService,
                new WellnessProperties("Africa/Casablanca", 2000),
                wellnessAiClient);
    }

    @Test
    void dashboardKeepsFrontendMetricShapeAndAddsAiFields() {
        DailyWellnessSummaryResponse daily = daily();
        WeeklyWellnessSummaryResponse weekly = weekly(daily);
        when(wellnessSummaryService.daily(eq(userId), any(LocalDate.class))).thenReturn(daily);
        when(wellnessSummaryService.weekly(eq(userId), any(LocalDate.class))).thenReturn(weekly);
        when(wellnessAiClient.lifestyleRisk(any()))
                .thenReturn(Optional.of(new LifestyleRiskAiResponse("MODERATE", 55.0, "RULE_BASED_BASELINE", List.of("LOW_SLEEP"), null)));
        when(wellnessAiClient.recommendations(any()))
                .thenReturn(Optional.of(new RecommendationAiResponse(
                        List.of(new RecommendationAiResponse.RecommendationAiItem("REST", "HIGH", "Consider a longer sleep window.")),
                        "RULE_BASED_BASELINE")));

        DashboardWellnessResponse response = dashboardService.getDashboardMetrics(userId);

        assertThat(response.sleep().value()).isEqualTo("7h 0m");
        assertThat(response.sleep().level()).isEqualTo(88);
        assertThat(response.riskLevel()).isEqualTo("MODERATE");
        assertThat(response.recommendations()).containsExactly("Consider a longer sleep window.");
    }

    @Test
    void dashboardStillWorksWhenAiIsDown() {
        DailyWellnessSummaryResponse daily = daily();
        when(wellnessSummaryService.daily(eq(userId), any(LocalDate.class))).thenReturn(daily);
        when(wellnessSummaryService.weekly(eq(userId), any(LocalDate.class))).thenReturn(weekly(daily));
        when(wellnessAiClient.lifestyleRisk(any())).thenReturn(Optional.empty());
        when(wellnessAiClient.recommendations(any())).thenReturn(Optional.empty());

        DashboardWellnessResponse response = dashboardService.getDashboardMetrics(userId);

        assertThat(response.hydration().value()).isEqualTo("1.8 L");
        assertThat(response.riskLevel()).isNull();
        assertThat(response.recommendations()).isEmpty();
    }

    private DailyWellnessSummaryResponse daily() {
        return new DailyWellnessSummaryResponse(
                LocalDate.of(2026, 9, 25),
                "Africa/Casablanca",
                new SleepSummary(420, 7.0, 8.0),
                new HydrationSummary(1800, 2000, 90.0),
                new NutritionSummary(3, 1800.0, 80.0, 200.0, 60.0),
                new ActivitySummary(1, 45, 200.0, 6000),
                new WellbeingSummary(7.0, 4.0, 3.0)
        );
    }

    private WeeklyWellnessSummaryResponse weekly(DailyWellnessSummaryResponse daily) {
        return new WeeklyWellnessSummaryResponse(
                daily.date(),
                daily.date(),
                daily.timezone(),
                420.0,
                8.0,
                1800.0,
                45,
                1,
                7.0,
                4.0,
                3.0,
                6000.0,
                1800.0,
                List.of(daily)
        );
    }
}
