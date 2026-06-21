package com.college.placement.modules.company.dto;

import com.college.placement.modules.company.domain.Company;
import com.college.placement.modules.company.domain.CompanyStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Company response")
public record CompanyResponse(
        UUID id,
        String name,
        String website,
        String industry,
        String description,
        String logoUrl,
        CompanyStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static CompanyResponse from(Company c) {
        return new CompanyResponse(
                c.getId(),
                c.getName(),
                c.getWebsite(),
                c.getIndustry(),
                c.getDescription(),
                c.getLogoUrl(),
                c.getStatus(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
