package com.college.placement.shared.academic.repository;

import com.college.placement.shared.academic.domain.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {

    Optional<AcademicYear> findByLabel(String label);

    Optional<AcademicYear> findByActiveTrue();

    boolean existsByLabel(String label);
}
