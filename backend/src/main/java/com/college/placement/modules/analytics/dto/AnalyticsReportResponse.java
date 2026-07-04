package com.college.placement.modules.analytics.dto;

import java.util.List;

/**
 * Aggregate placement-analytics report combining every section in a single
 * payload, suitable for a reporting dashboard's initial load.
 *
 * @param overview      headline KPIs
 * @param funnel        application pipeline by status
 * @param byBranch      per-branch placement outcomes
 * @param topRecruiters companies ranked by offers made
 * @param ctc           compensation distribution
 * @param trend         application/offer time series
 */
public record AnalyticsReportResponse(
        PlacementOverviewResponse overview,
        ApplicationFunnelResponse funnel,
        List<BranchPlacementStat> byBranch,
        List<RecruiterStat> topRecruiters,
        CtcStatsResponse ctc,
        List<TrendPoint> trend) {
}
