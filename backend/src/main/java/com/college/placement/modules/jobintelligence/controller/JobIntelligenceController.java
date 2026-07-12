package com.college.placement.modules.jobintelligence.controller;

import com.college.placement.modules.jobintelligence.dto.RunResponse;
import com.college.placement.modules.jobintelligence.dto.StartRunRequest;
import com.college.placement.modules.jobintelligence.service.JobIntelligenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Officer-facing API for the AI extraction pipeline. Starting a run returns 202
 * immediately — extraction is asynchronous; the frontend polls the run resource.
 */
@RestController
@RequestMapping("/api/job-intelligence")
@Tag(name = "Job Intelligence", description = "AI job posting analysis")
public class JobIntelligenceController {

    private final JobIntelligenceService jobIntelligenceService;
    private final ObjectMapper objectMapper;

    public JobIntelligenceController(JobIntelligenceService jobIntelligenceService,
                                     ObjectMapper objectMapper) {
        this.jobIntelligenceService = jobIntelligenceService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/runs")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Start an AI extraction run for a posting's official URL (async)")
    public ResponseEntity<RunResponse> startRun(@Valid @RequestBody StartRunRequest req,
                                                Authentication authentication) {
        var run = jobIntelligenceService.startRun(
                req.jobPostingId(), req.officialUrl(), authentication.getName());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(RunResponse.from(run, objectMapper));
    }

    @GetMapping("/runs/{id}")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Get run status/result by id (polled by the frontend)")
    public ResponseEntity<RunResponse> getRun(@PathVariable UUID id) {
        return ResponseEntity.ok(RunResponse.from(jobIntelligenceService.getRun(id), objectMapper));
    }

    @GetMapping("/postings/{postingId}/latest")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Latest run for a job posting (404 when none exists)")
    public ResponseEntity<RunResponse> latestForPosting(@PathVariable UUID postingId) {
        return ResponseEntity.ok(RunResponse.from(
                jobIntelligenceService.getLatestForPosting(postingId), objectMapper));
    }

    @PostMapping("/runs/{id}/retry")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Retry a failed run (same run row; tagging stays idempotent)")
    public ResponseEntity<RunResponse> retry(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(RunResponse.from(jobIntelligenceService.retry(id), objectMapper));
    }
}
