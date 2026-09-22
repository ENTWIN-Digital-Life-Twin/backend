package com.digitallifetwin.wellness.controller;

import com.digitallifetwin.wellness.dto.response.DashboardWellnessResponse;
import com.digitallifetwin.wellness.dto.response.WeeklyWellnessResponse;
import com.digitallifetwin.wellness.security.SecurityUtils;
import com.digitallifetwin.wellness.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wellness")
@RequiredArgsConstructor
@Tag(name = "Dashboard Wellness")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get today's wellness metrics for dashboard")
    public ResponseEntity<DashboardWellnessResponse> getDashboardMetrics() {
        return ResponseEntity.ok(dashboardService.getDashboardMetrics(SecurityUtils.currentUserId()));
    }

    @GetMapping("/weekly")
    @Operation(summary = "Get weekly wellness trends for dashboard")
    public ResponseEntity<WeeklyWellnessResponse> getWeeklyMetrics() {
        return ResponseEntity.ok(dashboardService.getWeeklyMetrics(SecurityUtils.currentUserId()));
    }
}
