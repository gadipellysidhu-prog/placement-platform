package com.college.placement.modules.company.dto;

import com.college.placement.modules.company.domain.JobPosting;
import com.college.placement.modules.company.domain.JobPostingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Job posting response")
public record JobPostingResponse(
        UUID id,
        UUID companyId,
        String companyName,
        String title,
        String description,
        BigDecimal ctcMin,
        BigDecimal ctcMax,
        JobPostingStatus status,
        LocalDate applicationDeadline,
        int offerLimit,
        @Schema(description = "Populated on detail responses; null on list responses")
        List<SkillRef> requiredSkills,
        @Schema(description = "Populated on detail responses; null on list responses")
        List<BranchRef> eligibleBranches,
        Instant createdAt,
        Instant updatedAt
) {

    @Schema(description = "Required skill reference")
    public record SkillRef(UUID id, String name) {}

    @Schema(description = "Eligible branch reference")
    public record BranchRef(UUID id, String name) {}

    /** List/summary projection — does not include skills or branches. */
    public static JobPostingResponse from(JobPosting jp) {
        return new JobPostingResponse(
                jp.getId(),
                jp.getCompany().getId(),
                jp.getCompany().getName(),
                jp.getTitle(),
                jp.getDescription(),
                jp.getCtcMin(),
                jp.getCtcMax(),
                jp.getStatus(),
                jp.getApplicationDeadline(),
                jp.getOfferLimit(),
                null,
                null,
                jp.getCreatedAt(),
                jp.getUpdatedAt()
        );
    }

    /**
     * Detail projection including required skills and eligible branches. The posting
     * must be loaded with those associations initialised (see
     * {@code JobPostingService.getDetailedById}).
     */
    public static JobPostingResponse fromDetailed(JobPosting jp) {
        List<SkillRef> skills = jp.getRequiredSkills().stream()
                .map(s -> new SkillRef(s.getId(), s.getName()))
                .toList();
        List<BranchRef> branches = jp.getEligibleBranches().stream()
                .map(b -> new BranchRef(b.getId(), b.getName()))
                .toList();
        return new JobPostingResponse(
                jp.getId(),
                jp.getCompany().getId(),
                jp.getCompany().getName(),
                jp.getTitle(),
                jp.getDescription(),
                jp.getCtcMin(),
                jp.getCtcMax(),
                jp.getStatus(),
                jp.getApplicationDeadline(),
                jp.getOfferLimit(),
                skills,
                branches,
                jp.getCreatedAt(),
                jp.getUpdatedAt()
        );
    }
}
