package com.college.placement.modules.dashboard.dto;

public record DashboardSummaryResponse(
        long totalStudents,
        long placedStudents,
        long activeCompanies,
        long openJobPostings,
        long pendingApplications,
        long pendingCertificates,
        double placementRatePercent
) {}
