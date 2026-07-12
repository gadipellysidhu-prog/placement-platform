package com.college.placement.modules.company.dto;

import com.college.placement.modules.company.domain.Recruiter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Recruiter profile response")
public record RecruiterResponse(
        UUID id,
        UUID userId,
        String userEmail,
        UUID companyId,
        String companyName,
        String designation,
        Instant createdAt,
        Instant updatedAt
) {
    public static RecruiterResponse from(Recruiter r) {
        return new RecruiterResponse(
                r.getId(),
                r.getUser().getId(),
                r.getUser().getEmail(),
                r.getCompany().getId(),
                r.getCompany().getName(),
                r.getDesignation(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
