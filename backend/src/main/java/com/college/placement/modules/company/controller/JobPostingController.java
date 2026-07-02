package com.college.placement.modules.company.controller;

import com.college.placement.modules.company.domain.JobPostingStatus;
import com.college.placement.modules.company.dto.JobPostingCreateRequest;
import com.college.placement.modules.company.dto.JobPostingResponse;
import com.college.placement.modules.company.dto.JobPostingUpdateRequest;
import com.college.placement.modules.company.service.JobPostingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/job-postings")
@Tag(name = "Job Postings", description = "Job posting lifecycle management")
public class JobPostingController {

    private final JobPostingService jobPostingService;

    public JobPostingController(JobPostingService jobPostingService) {
        this.jobPostingService = jobPostingService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Create a job posting (starts in DRAFT)")
    public ResponseEntity<JobPostingResponse> create(@Valid @RequestBody JobPostingCreateRequest req) {
        var posting = jobPostingService.createJobPosting(
                req.companyId(), req.title(), req.description(),
                req.ctcMin(), req.ctcMax(), req.applicationDeadline(), req.offerLimit());
        return ResponseEntity.status(HttpStatus.CREATED).body(JobPostingResponse.from(posting));
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "List open job postings (paginated), optionally filtered by title")
    public ResponseEntity<Page<JobPostingResponse>> listOpen(
            @RequestParam(required = false) String title, Pageable pageable) {
        return ResponseEntity.ok(jobPostingService.getOpenPostings(title, pageable).map(JobPostingResponse::from));
    }

    @GetMapping("/manage")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "List job postings across all statuses (officer), optionally filtered by status")
    public ResponseEntity<Page<JobPostingResponse>> listManage(
            @RequestParam(required = false) JobPostingStatus status, Pageable pageable) {
        return ResponseEntity.ok(
                jobPostingService.getManagedPostings(status, pageable).map(JobPostingResponse::from));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get job posting by ID (with required skills and eligible branches)")
    public ResponseEntity<JobPostingResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(JobPostingResponse.fromDetailed(jobPostingService.getDetailedById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Update a DRAFT job posting")
    public ResponseEntity<JobPostingResponse> update(@PathVariable UUID id,
                                                      @Valid @RequestBody JobPostingUpdateRequest req) {
        var posting = jobPostingService.updateJobPosting(id, req.title(), req.description(),
                req.ctcMin(), req.ctcMax(), req.applicationDeadline(), req.offerLimit());
        return ResponseEntity.ok(JobPostingResponse.from(posting));
    }

    @PostMapping("/{id}/open")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Open a DRAFT job posting for applications")
    public ResponseEntity<JobPostingResponse> open(@PathVariable UUID id) {
        return ResponseEntity.ok(JobPostingResponse.from(jobPostingService.openPosting(id)));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Close an OPEN job posting")
    public ResponseEntity<JobPostingResponse> close(@PathVariable UUID id) {
        return ResponseEntity.ok(JobPostingResponse.from(jobPostingService.closePosting(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Cancel a job posting")
    public ResponseEntity<JobPostingResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(JobPostingResponse.from(jobPostingService.cancelPosting(id)));
    }

    @PostMapping("/{id}/skills/{skillId}")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Add a required skill to a job posting")
    public ResponseEntity<JobPostingResponse> addSkill(@PathVariable UUID id, @PathVariable UUID skillId) {
        jobPostingService.addRequiredSkill(id, skillId);
        return ResponseEntity.ok(JobPostingResponse.fromDetailed(jobPostingService.getDetailedById(id)));
    }

    @DeleteMapping("/{id}/skills/{skillId}")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Remove a required skill from a job posting")
    public ResponseEntity<JobPostingResponse> removeSkill(@PathVariable UUID id, @PathVariable UUID skillId) {
        jobPostingService.removeRequiredSkill(id, skillId);
        return ResponseEntity.ok(JobPostingResponse.fromDetailed(jobPostingService.getDetailedById(id)));
    }

    @PostMapping("/{id}/branches/{branchId}")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Add an eligible branch to a job posting")
    public ResponseEntity<JobPostingResponse> addBranch(@PathVariable UUID id, @PathVariable UUID branchId) {
        jobPostingService.addEligibleBranch(id, branchId);
        return ResponseEntity.ok(JobPostingResponse.fromDetailed(jobPostingService.getDetailedById(id)));
    }

    @DeleteMapping("/{id}/branches/{branchId}")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Remove an eligible branch from a job posting")
    public ResponseEntity<JobPostingResponse> removeBranch(@PathVariable UUID id, @PathVariable UUID branchId) {
        jobPostingService.removeEligibleBranch(id, branchId);
        return ResponseEntity.ok(JobPostingResponse.fromDetailed(jobPostingService.getDetailedById(id)));
    }
}
