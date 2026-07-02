package com.college.placement.shared.settings.domain;

import com.college.placement.shared.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * A configuration-driven setting. Settings are either global
 * ({@code academicYearId == null}) or scoped to a specific academic year.
 * The stored {@code value} is always a string; {@link SettingValueType}
 * declares how callers should parse it.
 */
@Entity
@Table(
    name = "app_settings",
    indexes = @Index(name = "idx_app_settings_category", columnList = "category")
)
@Getter
@Setter
@NoArgsConstructor
public class AppSetting extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 150)
    @Column(name = "setting_key", nullable = false, length = 150)
    private String settingKey;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    private SettingValueType valueType = SettingValueType.STRING;

    @Size(max = 80)
    @Column(length = 80)
    private String category;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    /** Null for global settings; otherwise the owning academic year. */
    @Column(name = "academic_year_id")
    private UUID academicYearId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppSetting a)) return false;
        return id != null && id.equals(a.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
