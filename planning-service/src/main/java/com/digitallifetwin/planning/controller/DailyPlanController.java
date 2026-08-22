package com.digitallifetwin.planning.controller;

import com.digitallifetwin.planning.dto.response.DailyPlanResponse;
import com.digitallifetwin.planning.security.SecurityUtils;
import com.digitallifetwin.planning.service.DailyPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/planning")
@RequiredArgsConstructor
@Tag(name = "Daily Planning")
public class DailyPlanController {

    private final DailyPlanService dailyPlanService;

    @GetMapping("/daily")
    @Operation(summary = "Get calculated daily plan with free time and conflicts")
    public ResponseEntity<DailyPlanResponse> getDailyPlan(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(dailyPlanService.getDailyPlan(SecurityUtils.currentUserId(), date));
    }
}
