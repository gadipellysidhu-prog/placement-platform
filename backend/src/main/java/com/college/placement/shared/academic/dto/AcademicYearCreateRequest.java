package com.college.placement.shared.academic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Create a new academic year")
public record AcademicYearCreateRequest(
        @NotBlank @Size(max = 20) String label,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
