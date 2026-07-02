package com.college.placement.shared.academic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Update an academic year's dates")
public record AcademicYearUpdateRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
