package com.college.placement.modules.placement.service;

import com.college.placement.modules.company.domain.JobPosting;
import com.college.placement.modules.company.domain.JobPostingStatus;
import com.college.placement.modules.company.service.JobPostingService;
import com.college.placement.modules.placement.domain.ApplicationStatus;
import com.college.placement.modules.placement.domain.JobApplication;
import com.college.placement.modules.placement.event.ApplicationStatusChangedEvent;
import com.college.placement.modules.placement.repository.JobApplicationRepository;
import com.college.placement.modules.student.domain.Student;
import com.college.placement.modules.student.domain.StudentStatus;
import com.college.placement.modules.student.service.StudentService;
import com.college.placement.shared.eventbus.EventPublisher;
import com.college.placement.shared.eventbus.events.ApplicationSubmittedEvent;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

@Service
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final StudentService studentService;
    private final JobPostingService jobPostingService;
    private final EventPublisher eventPublisher;

    public JobApplicationService(JobApplicationRepository jobApplicationRepository,
                                 StudentService studentService,
                                 JobPostingService jobPostingService,
                                 EventPublisher eventPublisher) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.studentService = studentService;
        this.jobPostingService = jobPostingService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public JobApplication apply(UUID studentId, UUID jobPostingId) {
        Student student = studentService.getById(studentId);
        JobPosting posting = jobPostingService.getById(jobPostingId);

        if (!student.isPlacementEligible()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Student is not eligible for placement");
        }
        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Only active students can apply");
        }
        if (posting.getStatus() != JobPostingStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Job posting is not open for applications");
        }
        if (jobApplicationRepository.existsByStudentAndJobPosting(student, posting)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student has already applied to this job posting");
        }

        JobApplication application = new JobApplication();
        application.setStudent(student);
        application.setJobPosting(posting);
        JobApplication saved = jobApplicationRepository.save(application);
        eventPublisher.publish(ApplicationSubmittedEvent.of(
                saved.getId(), studentId, jobPostingId, posting.getCompany().getId()));
        return getById(saved.getId());
    }

    @Transactional
    public JobApplication updateStatus(UUID applicationId, ApplicationStatus newStatus) {
        JobApplication application = getById(applicationId);
        ApplicationStatus previous = application.getStatus();
        validateStatusTransition(previous, newStatus);
        application.setStatus(newStatus);
        jobApplicationRepository.save(application);
        eventPublisher.publish(ApplicationStatusChangedEvent.of(
                applicationId,
                application.getStudent().getId(),
                application.getJobPosting().getId(),
                previous,
                newStatus));
        return getById(applicationId);
    }

    @Transactional
    public JobApplication withdraw(UUID applicationId, UUID requesterStudentId) {
        JobApplication application = getById(applicationId);
        if (!application.getStudent().getId().equals(requesterStudentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only withdraw your own applications");
        }
        if (application.getStatus() == ApplicationStatus.OFFERED
                || application.getStatus() == ApplicationStatus.REJECTED
                || application.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cannot withdraw application in status: " + application.getStatus());
        }
        ApplicationStatus previous = application.getStatus();
        application.setStatus(ApplicationStatus.WITHDRAWN);
        jobApplicationRepository.save(application);
        eventPublisher.publish(ApplicationStatusChangedEvent.of(
                applicationId,
                application.getStudent().getId(),
                application.getJobPosting().getId(),
                previous,
                ApplicationStatus.WITHDRAWN));
        return getById(applicationId);
    }

    @Transactional(readOnly = true)
    public JobApplication getById(UUID id) {
        return jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    @Transactional(readOnly = true)
    public List<JobApplication> getByStudent(UUID studentId) {
        Student student = studentService.getById(studentId);
        return jobApplicationRepository.findByStudent(student);
    }

    @Transactional(readOnly = true)
    public List<JobApplication> getByJobPosting(UUID jobPostingId) {
        JobPosting posting = jobPostingService.getById(jobPostingId);
        return jobApplicationRepository.findByJobPosting(posting);
    }

    @Transactional(readOnly = true)
    public List<JobApplication> getByStatus(ApplicationStatus status) {
        return jobApplicationRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public Page<JobApplication> getAll(Pageable pageable) {
        return jobApplicationRepository.findAll(pageable);
    }

    private void validateStatusTransition(ApplicationStatus current, ApplicationStatus next) {
        boolean valid = switch (current) {
            case APPLIED -> next == ApplicationStatus.SHORTLISTED || next == ApplicationStatus.REJECTED
                    || next == ApplicationStatus.WITHDRAWN;
            case SHORTLISTED -> next == ApplicationStatus.INTERVIEWED || next == ApplicationStatus.REJECTED
                    || next == ApplicationStatus.WITHDRAWN;
            case INTERVIEWED -> next == ApplicationStatus.OFFERED || next == ApplicationStatus.REJECTED;
            case OFFERED, REJECTED, WITHDRAWN -> false;
        };
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Invalid application status transition from " + current + " to " + next);
        }
    }
}
