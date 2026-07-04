package com.college.placement.modules.analytics.dto;

/**
 * High-level placement KPIs. All figures are derived on-the-fly from the
 * operational tables (read model); no analytics data is persisted.
 *
 * @param totalStudents          all students on record
 * @param placedStudents         students whose status is {@code PLACED}
 * @param placementRatePercent   {@code placedStudents / totalStudents * 100}, one decimal
 * @param totalApplications      all job applications submitted
 * @param totalOffers            applications currently in {@code OFFERED} state
 * @param offerConversionPercent {@code totalOffers / totalApplications * 100}, one decimal
 * @param activeCompanies        companies with status {@code ACTIVE}
 * @param openPostings           job postings with status {@code OPEN}
 */
public record PlacementOverviewResponse(
        long totalStudents,
        long placedStudents,
        double placementRatePercent,
        long totalApplications,
        long totalOffers,
        double offerConversionPercent,
        long activeCompanies,
        long openPostings) {
}
