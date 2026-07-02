package com.college.placement.modules.company.service;

import com.college.placement.modules.company.domain.Company;
import com.college.placement.modules.company.domain.JobPosting;
import com.college.placement.modules.company.domain.JobPostingStatus;
import com.college.placement.modules.company.event.JobPostingClosedEvent;
import com.college.placement.modules.company.event.JobPostingOpenedEvent;
import com.college.placement.modules.company.repository.JobPostingRepository;
import com.college.placement.modules.student.domain.Branch;
import com.college.placement.modules.student.domain.Skill;
import com.college.placement.modules.student.service.BranchService;
import com.college.placement.modules.student.service.SkillService;
import com.college.placement.shared.eventbus.EventPublisher;
import com.college.placement.shared.eventbus.events.JobPostingCreatedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final CompanyService companyService;
    private final SkillService skillService;
    private final BranchService branchService;
    private final EventPublisher eventPublisher;

    public JobPostingService(JobPostingRepository jobPostingRepository,
                             CompanyService companyService,
                             SkillService skillService,
                             BranchService branchService,
                             EventPublisher eventPublisher) {
        this.jobPostingRepository = jobPostingRepository;
        this.companyService = companyService;
        this.skillService = skillService;
        this.branchService = branchService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public JobPosting createJobPosting(UUID companyId, String title, String description,
                                       BigDecimal ctcMin, BigDecimal ctcMax,
                                       LocalDate applicationDeadline, int offerLimit) {
        Company company = companyService.getById(companyId);
        JobPosting posting = new JobPosting();
        posting.setCompany(company);
        posting.setTitle(title);
        posting.setDescription(description);
        posting.setCtcMin(ctcMin);
        posting.setCtcMax(ctcMax);
        posting.setApplicationDeadline(applicationDeadline);
        posting.setOfferLimit(offerLimit);
        JobPosting saved = jobPostingRepository.save(posting);
        eventPublisher.publish(JobPostingCreatedEvent.of(saved.getId(), companyId, title));
        return saved;
    }

    @Transactional
    public JobPosting updateJobPosting(UUID id, String title, String description,
                                       BigDecimal ctcMin, BigDecimal ctcMax,
                                       LocalDate applicationDeadline, int offerLimit) {
        JobPosting posting = getById(id);
        if (posting.getStatus() != JobPostingStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Only DRAFT postings can be updated");
        }
        posting.setTitle(title);
        posting.setDescription(description);
        posting.setCtcMin(ctcMin);
        posting.setCtcMax(ctcMax);
        posting.setApplicationDeadline(applicationDeadline);
        posting.setOfferLimit(offerLimit);
        return jobPostingRepository.save(posting);
    }

    @Transactional
    public JobPosting openPosting(UUID id) {
        JobPosting posting = getById(id);
        if (posting.getStatus() != JobPostingStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Only DRAFT postings can be opened");
        }
        posting.setStatus(JobPostingStatus.OPEN);
        JobPosting saved = jobPostingRepository.save(posting);
        eventPublisher.publish(JobPostingOpenedEvent.of(id, posting.getCompany().getId(), posting.getTitle()));
        return saved;
    }

    @Transactional
    public JobPosting closePosting(UUID id) {
        JobPosting posting = getById(id);
        if (posting.getStatus() != JobPostingStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Only OPEN postings can be closed");
        }
        posting.setStatus(JobPostingStatus.CLOSED);
        JobPosting saved = jobPostingRepository.save(posting);
        eventPublisher.publish(JobPostingClosedEvent.of(id, posting.getCompany().getId(), posting.getTitle()));
        return saved;
    }

    @Transactional
    public JobPosting cancelPosting(UUID id) {
        JobPosting posting = getById(id);
        if (posting.getStatus() == JobPostingStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Posting is already cancelled");
        }
        posting.setStatus(JobPostingStatus.CANCELLED);
        return jobPostingRepository.save(posting);
    }

    @Transactional
    public JobPosting addRequiredSkill(UUID postingId, UUID skillId) {
        JobPosting posting = getById(postingId);
        Skill skill = skillService.getById(skillId);
        posting.getRequiredSkills().add(skill);
        return jobPostingRepository.save(posting);
    }

    @Transactional
    public JobPosting removeRequiredSkill(UUID postingId, UUID skillId) {
        JobPosting posting = getById(postingId);
        Skill skill = skillService.getById(skillId);
        posting.getRequiredSkills().remove(skill);
        return jobPostingRepository.save(posting);
    }

    @Transactional
    public JobPosting addEligibleBranch(UUID postingId, UUID branchId) {
        JobPosting posting = getById(postingId);
        Branch branch = branchService.getById(branchId);
        posting.getEligibleBranches().add(branch);
        return jobPostingRepository.save(posting);
    }

    @Transactional
    public JobPosting removeEligibleBranch(UUID postingId, UUID branchId) {
        JobPosting posting = getById(postingId);
        Branch branch = branchService.getById(branchId);
        posting.getEligibleBranches().remove(branch);
        return jobPostingRepository.save(posting);
    }

    @Transactional(readOnly = true)
    public JobPosting getById(UUID id) {
        return jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job posting not found"));
    }

    /** Loads a posting with its required skills and eligible branches initialised for detail responses. */
    @Transactional(readOnly = true)
    public JobPosting getDetailedById(UUID id) {
        return jobPostingRepository.findDetailedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job posting not found"));
    }

    /** Officer-facing listing across all statuses, optionally filtered by a single status. */
    @Transactional(readOnly = true)
    public Page<JobPosting> getManagedPostings(JobPostingStatus status, Pageable pageable) {
        return status == null
                ? jobPostingRepository.findAll(pageable)
                : jobPostingRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public List<JobPosting> getByCompany(UUID companyId) {
        Company company = companyService.getById(companyId);
        return jobPostingRepository.findByCompany(company);
    }

    @Transactional(readOnly = true)
    public Page<JobPosting> getOpenPostings(Pageable pageable) {
        return jobPostingRepository.findByStatus(JobPostingStatus.OPEN, pageable);
    }

    /** Open postings, optionally filtered by a case-insensitive title substring. */
    @Transactional(readOnly = true)
    public Page<JobPosting> getOpenPostings(String title, Pageable pageable) {
        return (title == null || title.isBlank())
                ? jobPostingRepository.findByStatus(JobPostingStatus.OPEN, pageable)
                : jobPostingRepository.findByStatusAndTitleContainingIgnoreCase(
                        JobPostingStatus.OPEN, title, pageable);
    }
}
