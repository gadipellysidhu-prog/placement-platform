package com.college.placement.modules.placement.dto;

import com.college.placement.modules.placement.domain.ApplicationStatus;
import com.college.placement.modules.placement.domain.JobApplication;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Job application response")
public record JobApplicationResponse(
        UUID id,
        UUID studentId,
        String studentRollNumber,
        UUID jobPostingId,
        String jobPostingTitle,
        UUID companyId,
        String companyName,
        ApplicationStatus status,
        Instant appliedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static JobApplicationResponse from(JobApplication a) {
        return new JobApplicationResponse(
                a.getId(),
                a.getStudent().getId(),
                a.getStudent().getRollNumber(),
                a.getJobPosting().getId(),
                a.getJobPosting().getTitle(),
                a.getJobPosting().getCompany().getId(),
                a.getJobPosting().getCompany().getName(),
                a.getStatus(),
                a.getAppliedAt(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
