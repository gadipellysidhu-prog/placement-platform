package com.college.placement.modules.company.repository;

import com.college.placement.modules.company.domain.Company;
import com.college.placement.modules.company.domain.JobPosting;
import com.college.placement.modules.company.domain.JobPostingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobPostingRepository extends JpaRepository<JobPosting, UUID> {

    List<JobPosting> findByCompany(Company company);

    Page<JobPosting> findByStatus(JobPostingStatus status, Pageable pageable);

    List<JobPosting> findByCompanyAndStatus(Company company, JobPostingStatus status);
}
