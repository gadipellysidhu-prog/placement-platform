package com.college.placement.modules.placement.repository;

import com.college.placement.modules.company.domain.JobPosting;
import com.college.placement.modules.placement.domain.ApplicationStatus;
import com.college.placement.modules.placement.domain.JobApplication;
import com.college.placement.modules.student.domain.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

    @EntityGraph(attributePaths = {"student", "jobPosting", "jobPosting.company"})
    Optional<JobApplication> findById(UUID id);

    @EntityGraph(attributePaths = {"student", "jobPosting", "jobPosting.company"})
    Page<JobApplication> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"student", "jobPosting", "jobPosting.company"})
    List<JobApplication> findByStudent(Student student);

    boolean existsByStudentAndJobPosting(Student student, JobPosting jobPosting);

    Optional<JobApplication> findByStudentAndJobPosting(Student student, JobPosting jobPosting);

    List<JobApplication> findByJobPosting(JobPosting jobPosting);

    List<JobApplication> findByStatus(ApplicationStatus status);

    long countByStatus(ApplicationStatus status);
}
