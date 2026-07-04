package com.college.placement.modules.analytics.service;

import com.college.placement.modules.analytics.dto.AnalyticsReportResponse;
import com.college.placement.modules.analytics.dto.ApplicationFunnelResponse;
import com.college.placement.modules.analytics.dto.BranchPlacementStat;
import com.college.placement.modules.analytics.dto.CtcStatsResponse;
import com.college.placement.modules.analytics.dto.PlacementOverviewResponse;
import com.college.placement.modules.analytics.dto.RecruiterStat;
import com.college.placement.modules.analytics.dto.TrendPoint;
import com.college.placement.modules.analytics.repository.AnalyticsRepository;
import com.college.placement.modules.company.domain.CompanyStatus;
import com.college.placement.modules.company.domain.JobPostingStatus;
import com.college.placement.modules.company.repository.CompanyRepository;
import com.college.placement.modules.company.repository.JobPostingRepository;
import com.college.placement.modules.placement.domain.ApplicationStatus;
import com.college.placement.modules.placement.repository.JobApplicationRepository;
import com.college.placement.modules.student.domain.StudentStatus;
import com.college.placement.modules.student.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Placement analytics read model. Computes reporting aggregates on-the-fly from
 * the operational tables; nothing is persisted. Read-only and side-effect free,
 * so it is safe to call from any query path.
 *
 * <p>Follows the established cross-module read pattern (see {@code DashboardController}):
 * analytics may read other modules' repositories but never writes across a boundary.
 */
@Service
public class AnalyticsService {

    /** Upper bound on the trend window to keep query/response size bounded. */
    private static final int MAX_TREND_MONTHS = 24;
    private static final int DEFAULT_TREND_MONTHS = 6;
    private static final int MAX_RECRUITERS = 100;

    private final AnalyticsRepository analyticsRepository;
    private final StudentRepository studentRepository;
    private final JobApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final JobPostingRepository jobPostingRepository;

    public AnalyticsService(AnalyticsRepository analyticsRepository,
                            StudentRepository studentRepository,
                            JobApplicationRepository applicationRepository,
                            CompanyRepository companyRepository,
                            JobPostingRepository jobPostingRepository) {
        this.analyticsRepository = analyticsRepository;
        this.studentRepository = studentRepository;
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
        this.jobPostingRepository = jobPostingRepository;
    }

    @Transactional(readOnly = true)
    public PlacementOverviewResponse overview() {
        long totalStudents = studentRepository.count();
        long placedStudents = studentRepository.countByStatus(StudentStatus.PLACED);
        long totalApplications = applicationRepository.count();
        long totalOffers = applicationRepository.countByStatus(ApplicationStatus.OFFERED);
        long activeCompanies = companyRepository.countByStatus(CompanyStatus.ACTIVE);
        long openPostings = jobPostingRepository.countByStatus(JobPostingStatus.OPEN);
        return new PlacementOverviewResponse(
                totalStudents,
                placedStudents,
                percent(placedStudents, totalStudents),
                totalApplications,
                totalOffers,
                percent(totalOffers, totalApplications),
                activeCompanies,
                openPostings);
    }

    @Transactional(readOnly = true)
    public ApplicationFunnelResponse funnel() {
        Map<ApplicationStatus, Long> raw = analyticsRepository.applicationCountsByStatus();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        long total = 0L;
        // Zero-fill every status in declaration order so the funnel is stable.
        for (ApplicationStatus status : ApplicationStatus.values()) {
            long count = raw.getOrDefault(status, 0L);
            byStatus.put(status.name(), count);
            total += count;
        }
        return new ApplicationFunnelResponse(byStatus, total);
    }

    @Transactional(readOnly = true)
    public List<BranchPlacementStat> placementByBranch() {
        List<BranchPlacementStat> stats = new ArrayList<>();
        for (AnalyticsRepository.BranchAggregate agg : analyticsRepository.branchPlacementAggregates()) {
            stats.add(new BranchPlacementStat(
                    agg.branch(),
                    agg.totalStudents(),
                    agg.placedStudents(),
                    percent(agg.placedStudents(), agg.totalStudents())));
        }
        return stats;
    }

    @Transactional(readOnly = true)
    public List<RecruiterStat> topRecruiters(int limit) {
        int bounded = Math.max(1, Math.min(limit, MAX_RECRUITERS));
        return analyticsRepository.topRecruiters(bounded);
    }

    @Transactional(readOnly = true)
    public CtcStatsResponse ctcStats() {
        AnalyticsRepository.CtcAggregate agg = analyticsRepository.ctcAggregate();
        BigDecimal avg = agg.avg() == null ? null : agg.avg().setScale(2, RoundingMode.HALF_UP);
        return new CtcStatsResponse(agg.min(), agg.max(), avg, agg.count());
    }

    @Transactional(readOnly = true)
    public List<TrendPoint> applicationTrend(int months) {
        int window = months <= 0 ? DEFAULT_TREND_MONTHS : Math.min(months, MAX_TREND_MONTHS);
        YearMonth current = YearMonth.now(ZoneOffset.UTC);
        YearMonth start = current.minusMonths(window - 1L);
        Instant cutoff = start.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        // Pre-seed every month in the window so gaps render as zero.
        Map<YearMonth, long[]> buckets = new LinkedHashMap<>();
        for (YearMonth ym = start; !ym.isAfter(current); ym = ym.plusMonths(1)) {
            buckets.put(ym, new long[2]);
        }

        for (AnalyticsRepository.ApplicationTimePoint point : analyticsRepository.applicationsSince(cutoff)) {
            YearMonth ym = YearMonth.from(point.appliedAt().atZone(ZoneOffset.UTC));
            long[] counts = buckets.get(ym);
            if (counts == null) {
                continue; // defensive: outside the seeded window
            }
            counts[0]++;
            if (point.status() == ApplicationStatus.OFFERED) {
                counts[1]++;
            }
        }

        List<TrendPoint> trend = new ArrayList<>(buckets.size());
        buckets.forEach((ym, counts) -> trend.add(new TrendPoint(ym.toString(), counts[0], counts[1])));
        return trend;
    }

    @Transactional(readOnly = true)
    public AnalyticsReportResponse report(int recruiterLimit, int trendMonths) {
        return new AnalyticsReportResponse(
                overview(),
                funnel(),
                placementByBranch(),
                topRecruiters(recruiterLimit),
                ctcStats(),
                applicationTrend(trendMonths));
    }

    /** {@code part / whole * 100} rounded to one decimal; 0 when {@code whole == 0}. */
    private static double percent(long part, long whole) {
        if (whole <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(part * 100.0 / whole).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
