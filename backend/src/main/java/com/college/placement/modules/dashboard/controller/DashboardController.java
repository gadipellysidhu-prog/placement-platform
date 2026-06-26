package com.college.placement.modules.dashboard.controller;

import com.college.placement.modules.certificate.domain.CertificateVerificationStatus;
import com.college.placement.modules.certificate.repository.CertificateRepository;
import com.college.placement.modules.company.domain.CompanyStatus;
import com.college.placement.modules.company.domain.JobPostingStatus;
import com.college.placement.modules.company.repository.CompanyRepository;
import com.college.placement.modules.company.repository.JobPostingRepository;
import com.college.placement.modules.dashboard.dto.DashboardSummaryResponse;
import com.college.placement.modules.placement.domain.ApplicationStatus;
import com.college.placement.modules.placement.repository.JobApplicationRepository;
import com.college.placement.modules.student.domain.StudentStatus;
import com.college.placement.modules.student.repository.StudentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Aggregate statistics for the placement dashboard")
public class DashboardController {

    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;
    private final JobPostingRepository jobPostingRepository;
    private final JobApplicationRepository applicationRepository;
    private final CertificateRepository certificateRepository;

    public DashboardController(StudentRepository studentRepository,
                                CompanyRepository companyRepository,
                                JobPostingRepository jobPostingRepository,
                                JobApplicationRepository applicationRepository,
                                CertificateRepository certificateRepository) {
        this.studentRepository = studentRepository;
        this.companyRepository = companyRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.applicationRepository = applicationRepository;
        this.certificateRepository = certificateRepository;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER','ROLE_ADMIN')")
    @Operation(summary = "Get placement dashboard summary statistics")
    public ResponseEntity<DashboardSummaryResponse> summary() {
        long totalStudents = studentRepository.count();
        long placedStudents = studentRepository.countByStatus(StudentStatus.PLACED);
        long activeCompanies = companyRepository.countByStatus(CompanyStatus.ACTIVE);
        long openJobPostings = jobPostingRepository.countByStatus(JobPostingStatus.OPEN);
        long pendingApplications = applicationRepository.countByStatus(ApplicationStatus.APPLIED);
        long pendingCertificates = certificateRepository.countByVerificationStatus(CertificateVerificationStatus.PENDING);
        double placementRate = totalStudents > 0
                ? Math.round((double) placedStudents / totalStudents * 1000.0) / 10.0
                : 0.0;
        return ResponseEntity.ok(new DashboardSummaryResponse(
                totalStudents, placedStudents, activeCompanies,
                openJobPostings, pendingApplications, pendingCertificates, placementRate));
    }
}
