package com.college.placement.modules.company.repository;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.company.domain.Company;
import com.college.placement.modules.company.domain.Recruiter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecruiterRepository extends JpaRepository<Recruiter, UUID> {

    Optional<Recruiter> findByUser(AppUser user);

    boolean existsByUser(AppUser user);

    List<Recruiter> findByCompany(Company company);
}
