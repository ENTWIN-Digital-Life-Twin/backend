package com.digitallifetwin.wellness.controller;

import com.digitallifetwin.wellness.dto.response.DailyWellnessSummaryResponse;
import com.digitallifetwin.wellness.dto.response.WeeklyWellnessSummaryResponse;
import com.digitallifetwin.wellness.security.SecurityUtils;
import com.digitallifetwin.wellness.service.summary.WellnessSummaryService;
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
@RequestMapping("/api/v1/wellness/summary")
@RequiredArgsConstructor
@Tag(name = "Wellness summary")
public class WellnessSummaryController {

    private final WellnessSummaryService wellnessSummaryService;

    @GetMapping("/daily")
    @Operation(summary = "Daily wellness summary for a local date")
    public ResponseEntity<DailyWellnessSummaryResponse> daily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(wellnessSummaryService.daily(SecurityUtils.currentUserId(), date));
    }

    @GetMapping("/weekly")
    @Operation(summary = "Weekly wellness summary starting on the given local date")
    public ResponseEntity<WeeklyWellnessSummaryResponse> weekly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        return ResponseEntity.ok(wellnessSummaryService.weekly(SecurityUtils.currentUserId(), startDate));
    }
}
