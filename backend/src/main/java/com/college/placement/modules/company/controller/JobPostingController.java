package com.college.placement.modules.company.controller;

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
    @Operation(summary = "List open job postings (paginated)")
    public ResponseEntity<Page<JobPostingResponse>> listOpen(Pageable pageable) {
        return ResponseEntity.ok(jobPostingService.getOpenPostings(pageable).map(JobPostingResponse::from));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get job posting by ID")
    public ResponseEntity<JobPostingResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(JobPostingResponse.from(jobPostingService.getById(id)));
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
}
