package com.digitallifetwin.planning.controller;

import com.digitallifetwin.planning.dto.response.DashboardStatsResponse;
import com.digitallifetwin.planning.dto.response.TimelineEventResponse;
import com.digitallifetwin.planning.dto.response.UpcomingEventResponse;
import com.digitallifetwin.planning.security.SecurityUtils;
import com.digitallifetwin.planning.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics for today")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        return ResponseEntity.ok(dashboardService.getStats(SecurityUtils.currentUserId()));
    }

    @GetMapping("/timeline")
    @Operation(summary = "Get today's timeline from tasks and events")
    public ResponseEntity<List<TimelineEventResponse>> getTimeline() {
        return ResponseEntity.ok(dashboardService.getTimeline(SecurityUtils.currentUserId()));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get the next upcoming event")
    public ResponseEntity<UpcomingEventResponse> getUpcomingEvent() {
        UpcomingEventResponse event = dashboardService.getUpcomingEvent(SecurityUtils.currentUserId());
        if (event == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(event);
    }
}
