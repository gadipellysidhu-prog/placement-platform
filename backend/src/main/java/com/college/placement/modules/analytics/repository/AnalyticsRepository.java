package com.college.placement.modules.analytics.repository;

import com.college.placement.modules.analytics.dto.RecruiterStat;
import com.college.placement.modules.placement.domain.ApplicationStatus;
import com.college.placement.modules.student.domain.StudentStatus;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only aggregation queries backing the analytics module. Uses the JPA
 * {@link EntityManager} directly so grouping/aggregation happens in the database
 * (no entity hydration). Owns its own queries rather than leaking reporting
 * concerns into the operational per-entity repositories.
 */
@Repository
public class AnalyticsRepository {

    private final EntityManager em;

    public AnalyticsRepository(EntityManager em) {
        this.em = em;
    }

    /** Application counts grouped by status (only non-zero buckets are returned). */
    public Map<ApplicationStatus, Long> applicationCountsByStatus() {
        List<Object[]> rows = em.createQuery(
                        "SELECT a.status, COUNT(a) FROM JobApplication a GROUP BY a.status", Object[].class)
                .getResultList();
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (Object[] row : rows) {
            counts.put((ApplicationStatus) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    /** Per-branch student totals and placed counts; {@code null} branch → "Unassigned". */
    public List<BranchAggregate> branchPlacementAggregates() {
        List<Object[]> rows = em.createQuery(
                        "SELECT b.name, COUNT(s), "
                                + "SUM(CASE WHEN s.status = :placed THEN 1 ELSE 0 END) "
                                + "FROM Student s LEFT JOIN s.branch b GROUP BY b.name ORDER BY b.name",
                        Object[].class)
                .setParameter("placed", StudentStatus.PLACED)
                .getResultList();
        List<BranchAggregate> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            String branch = row[0] != null ? (String) row[0] : "Unassigned";
            long total = ((Number) row[1]).longValue();
            long placed = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            result.add(new BranchAggregate(branch, total, placed));
        }
        return result;
    }

    /**
     * Companies ranked by offers made (then by total applications), limited to
     * {@code limit}. Grouping is done in the database; ordering/limiting in memory
     * to stay portable across JPA providers (the candidate set is small).
     */
    public List<RecruiterStat> topRecruiters(int limit) {
        List<Object[]> rows = em.createQuery(
                        "SELECT c.name, "
                                + "SUM(CASE WHEN a.status = :offered THEN 1 ELSE 0 END), "
                                + "COUNT(a) "
                                + "FROM JobApplication a JOIN a.jobPosting p JOIN p.company c GROUP BY c.name",
                        Object[].class)
                .setParameter("offered", ApplicationStatus.OFFERED)
                .getResultList();
        List<RecruiterStat> stats = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            long offers = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            long total = ((Number) row[2]).longValue();
            stats.add(new RecruiterStat((String) row[0], offers, total));
        }
        stats.sort(Comparator.comparingLong(RecruiterStat::offers)
                .thenComparingLong(RecruiterStat::totalApplications).reversed());
        return limit > 0 && stats.size() > limit ? new ArrayList<>(stats.subList(0, limit)) : stats;
    }

    /** Min/max/avg CTC across postings that declare a {@code ctcMax}. */
    public CtcAggregate ctcAggregate() {
        Object[] row = em.createQuery(
                        "SELECT MIN(p.ctcMin), MAX(p.ctcMax), AVG(p.ctcMax), COUNT(p) "
                                + "FROM JobPosting p WHERE p.ctcMax IS NOT NULL", Object[].class)
                .getSingleResult();
        long count = row[3] != null ? ((Number) row[3]).longValue() : 0L;
        BigDecimal avg = row[2] != null ? BigDecimal.valueOf(((Number) row[2]).doubleValue()) : null;
        return new CtcAggregate((BigDecimal) row[0], (BigDecimal) row[1], avg, count);
    }

    /** Raw (appliedAt, status) pairs for applications on or after {@code cutoff}. */
    public List<ApplicationTimePoint> applicationsSince(Instant cutoff) {
        List<Object[]> rows = em.createQuery(
                        "SELECT a.appliedAt, a.status FROM JobApplication a WHERE a.appliedAt >= :cutoff",
                        Object[].class)
                .setParameter("cutoff", cutoff)
                .getResultList();
        List<ApplicationTimePoint> points = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            points.add(new ApplicationTimePoint((Instant) row[0], (ApplicationStatus) row[1]));
        }
        return points;
    }

    /** Per-branch raw aggregate (rate is derived in the service layer). */
    public record BranchAggregate(String branch, long totalStudents, long placedStudents) {
    }

    /** CTC aggregate carrier; {@code avg} pre-rounding. */
    public record CtcAggregate(BigDecimal min, BigDecimal max, BigDecimal avg, long count) {
    }

    /** A single application's timestamp and current status, for time-series bucketing. */
    public record ApplicationTimePoint(Instant appliedAt, ApplicationStatus status) {
    }
}
