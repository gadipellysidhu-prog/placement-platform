package com.college.placement.modules.company.repository;

import com.college.placement.modules.company.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByName(String name);

    boolean existsByName(String name);

    List<Company> findByIndustry(String industry);

    long countByStatus(com.college.placement.modules.company.domain.CompanyStatus status);
}
