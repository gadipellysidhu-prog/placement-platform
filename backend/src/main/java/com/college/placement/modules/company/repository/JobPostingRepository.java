package com.college.placement.modules.company.repository;

import com.college.placement.modules.company.domain.Company;
import com.college.placement.modules.company.domain.JobPosting;
import com.college.placement.modules.company.domain.JobPostingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobPostingRepository extends JpaRepository<JobPosting, UUID> {

    @EntityGraph(attributePaths = {"company"})
    Optional<JobPosting> findById(UUID id);

    @EntityGraph(attributePaths = {"company"})
    Page<JobPosting> findByStatus(JobPostingStatus status, Pageable pageable);

    List<JobPosting> findByCompany(Company company);

    List<JobPosting> findByCompanyAndStatus(Company company, JobPostingStatus status);

    long countByStatus(JobPostingStatus status);
}
