package com.college.placement.modules.analytics.controller;

import com.college.placement.modules.analytics.dto.AnalyticsReportResponse;
import com.college.placement.modules.analytics.dto.ApplicationFunnelResponse;
import com.college.placement.modules.analytics.dto.BranchPlacementStat;
import com.college.placement.modules.analytics.dto.CtcStatsResponse;
import com.college.placement.modules.analytics.dto.PlacementOverviewResponse;
import com.college.placement.modules.analytics.dto.RecruiterStat;
import com.college.placement.modules.analytics.dto.TrendPoint;
import com.college.placement.modules.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Placement-analytics reporting API. All endpoints are read-only aggregates and
 * are restricted to placement officers (and, via the role hierarchy,
 * administrators). Mirrors the authorization posture of the dashboard.
 */
@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Placement analytics & reporting (placement officer)")
@PreAuthorize("hasRole('PLACEMENT_OFFICER')")
public class AnalyticsController {

    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/report")
    @Operation(summary = "Full analytics report (overview, funnel, branches, recruiters, CTC, trend)")
    public ResponseEntity<AnalyticsReportResponse> report(
            @RequestParam(name = "recruiterLimit", defaultValue = "10") int recruiterLimit,
            @RequestParam(name = "trendMonths", defaultValue = "6") int trendMonths) {
        return ResponseEntity.ok(service.report(recruiterLimit, trendMonths));
    }

    @GetMapping("/overview")
    @Operation(summary = "Headline placement KPIs")
    public ResponseEntity<PlacementOverviewResponse> overview() {
        return ResponseEntity.ok(service.overview());
    }

    @GetMapping("/funnel")
    @Operation(summary = "Application pipeline counts by status")
    public ResponseEntity<ApplicationFunnelResponse> funnel() {
        return ResponseEntity.ok(service.funnel());
    }

    @GetMapping("/by-branch")
    @Operation(summary = "Placement outcomes per branch")
    public ResponseEntity<List<BranchPlacementStat>> byBranch() {
        return ResponseEntity.ok(service.placementByBranch());
    }

    @GetMapping("/top-recruiters")
    @Operation(summary = "Companies ranked by offers made")
    public ResponseEntity<List<RecruiterStat>> topRecruiters(
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(service.topRecruiters(limit));
    }

    @GetMapping("/ctc")
    @Operation(summary = "CTC (compensation) distribution across postings")
    public ResponseEntity<CtcStatsResponse> ctc() {
        return ResponseEntity.ok(service.ctcStats());
    }

    @GetMapping("/trend")
    @Operation(summary = "Monthly application/offer time series")
    public ResponseEntity<List<TrendPoint>> trend(
            @RequestParam(name = "months", defaultValue = "6") int months) {
        return ResponseEntity.ok(service.applicationTrend(months));
    }
}
