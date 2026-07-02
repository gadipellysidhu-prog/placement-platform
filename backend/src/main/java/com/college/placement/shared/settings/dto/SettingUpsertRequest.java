package com.college.placement.shared.settings.dto;

import com.college.placement.shared.settings.domain.SettingValueType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Create or update a configuration setting")
public record SettingUpsertRequest(
        @NotBlank @Size(max = 150) String settingKey,
        @Size(max = 100_000) String settingValue,
        @NotNull SettingValueType valueType,
        @Size(max = 80) String category,
        @Size(max = 500) String description,
        @Schema(description = "Null for a global setting; otherwise the owning academic year")
        UUID academicYearId
) {
}
