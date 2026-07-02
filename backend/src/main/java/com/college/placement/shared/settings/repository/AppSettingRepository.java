package com.college.placement.shared.settings.repository;

import com.college.placement.shared.settings.domain.AppSetting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppSettingRepository extends JpaRepository<AppSetting, UUID> {

    Optional<AppSetting> findBySettingKeyAndAcademicYearIdIsNull(String settingKey);

    Optional<AppSetting> findBySettingKeyAndAcademicYearId(String settingKey, UUID academicYearId);

    Page<AppSetting> findByCategory(String category, Pageable pageable);
}
