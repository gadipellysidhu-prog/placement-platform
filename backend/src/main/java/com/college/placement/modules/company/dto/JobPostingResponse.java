package com.college.placement.modules.company.dto;

import com.college.placement.modules.company.domain.JobPosting;
import com.college.placement.modules.company.domain.JobPostingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
        Instant createdAt,
        Instant updatedAt
) {
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
                jp.getCreatedAt(),
                jp.getUpdatedAt()
        );
    }
}
