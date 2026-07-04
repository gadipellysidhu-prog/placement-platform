package com.college.placement.modules.analytics.dto;

/**
 * Placement outcomes for a single branch.
 *
 * @param branch               branch name ({@code "Unassigned"} for students with no branch)
 * @param totalStudents        students in the branch
 * @param placedStudents       students in the branch with status {@code PLACED}
 * @param placementRatePercent {@code placedStudents / totalStudents * 100}, one decimal
 */
public record BranchPlacementStat(
        String branch,
        long totalStudents,
        long placedStudents,
        double placementRatePercent) {
}
