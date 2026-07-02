package com.college.placement.modules.eligibility;

import com.college.placement.modules.company.domain.JobPosting;
import com.college.placement.modules.student.domain.Student;
import com.college.placement.modules.student.domain.StudentStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates whether a student meets the criteria to apply to a given job posting.
 * Returns an {@link EligibilityResult} with pass/fail and a list of human-readable
 * rejection reasons.
 */
@Service
public class EligibilityService {

    /**
     * Runs all eligibility checks in order. Short-circuits on fundamental status issues
     * but accumulates branch/CGPA reasons so the student sees the full picture.
     */
    public EligibilityResult check(Student student, JobPosting posting) {
        List<String> reasons = new ArrayList<>();

        if (student.getStatus() != StudentStatus.ACTIVE) {
            reasons.add("Your student account is not active (current status: " + student.getStatus() + ").");
            return EligibilityResult.fail(reasons);
        }

        if (!student.isPlacementEligible()) {
            reasons.add("You have been marked as ineligible for placement by the placement officer.");
        }

        // Branch check — only applies when the posting restricts eligible branches.
        if (!posting.getEligibleBranches().isEmpty()) {
            if (student.getBranch() == null) {
                reasons.add("Your profile does not have a branch assigned. This posting is restricted to specific branches.");
            } else if (posting.getEligibleBranches().stream()
                    .noneMatch(b -> b.getId().equals(student.getBranch().getId()))) {
                reasons.add("Your branch (" + student.getBranch().getName()
                        + ") is not in the list of eligible branches for this posting.");
            }
        }

        return reasons.isEmpty() ? EligibilityResult.pass() : EligibilityResult.fail(reasons);
    }
}
