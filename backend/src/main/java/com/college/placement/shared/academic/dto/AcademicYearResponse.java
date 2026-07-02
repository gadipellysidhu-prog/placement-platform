package com.college.placement.shared.academic.dto;

import com.college.placement.shared.academic.domain.AcademicYear;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Academic year / placement season")
public record AcademicYearResponse(
        UUID id,
        String label,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static AcademicYearResponse from(AcademicYear y) {
        return new AcademicYearResponse(
                y.getId(),
                y.getLabel(),
                y.getStartDate(),
                y.getEndDate(),
                y.isActive(),
                y.getCreatedAt(),
                y.getUpdatedAt()
        );
    }
}
