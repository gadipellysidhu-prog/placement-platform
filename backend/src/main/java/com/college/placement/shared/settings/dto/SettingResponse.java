package com.college.placement.shared.settings.dto;

import com.college.placement.shared.settings.domain.AppSetting;
import com.college.placement.shared.settings.domain.SettingValueType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Configuration setting")
public record SettingResponse(
        UUID id,
        String settingKey,
        String settingValue,
        SettingValueType valueType,
        String category,
        String description,
        UUID academicYearId,
        Instant createdAt,
        Instant updatedAt
) {
    public static SettingResponse from(AppSetting s) {
        return new SettingResponse(
                s.getId(),
                s.getSettingKey(),
                s.getSettingValue(),
                s.getValueType(),
                s.getCategory(),
                s.getDescription(),
                s.getAcademicYearId(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
