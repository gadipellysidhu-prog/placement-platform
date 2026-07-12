package com.college.placement.modules.jobintelligence.service;

import com.college.placement.modules.company.service.JobPostingService;
import com.college.placement.modules.jobintelligence.configuration.JobIntelligenceFlags;
import com.college.placement.modules.jobintelligence.crawler.UrlValidator;
import com.college.placement.modules.jobintelligence.domain.JobIntelligenceRun;
import com.college.placement.modules.jobintelligence.domain.RunStatus;
import com.college.placement.modules.jobintelligence.events.JobIntelligenceRequestedEvent;
import com.college.placement.modules.jobintelligence.repository.JobIntelligenceRunRepository;
import com.college.placement.shared.eventbus.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Run lifecycle: starting, querying, retrying, and progress updates. The heavy
 * pipeline work happens asynchronously (see JobIntelligenceEventListener) — this
 * service only creates the run row and publishes the trigger event, so the HTTP
 * request returns immediately.
 */
@Slf4j
@Service
public class JobIntelligenceService {

    private static final List<RunStatus> ACTIVE_STATUSES = List.of(
            RunStatus.PENDING, RunStatus.FETCHING, RunStatus.EXTRACTING,
            RunStatus.NORMALIZING, RunStatus.TAGGING, RunStatus.PREDICTING_BRANCHES);

    private final JobIntelligenceRunRepository runRepository;
    private final JobPostingService jobPostingService;
    private final UrlValidator urlValidator;
    private final JobIntelligenceFlags flags;
    private final EventPublisher eventPublisher;

    public JobIntelligenceService(JobIntelligenceRunRepository runRepository,
                                  JobPostingService jobPostingService,
                                  UrlValidator urlValidator,
                                  JobIntelligenceFlags flags,
                                  EventPublisher eventPublisher) {
        this.runRepository = runRepository;
        this.jobPostingService = jobPostingService;
        this.urlValidator = urlValidator;
        this.flags = flags;
        this.eventPublisher = eventPublisher;
    }

    /** Start an AI extraction run for a posting. Returns immediately; work is async. */
    @Transactional
    public JobIntelligenceRun startRun(UUID jobPostingId, String officialUrl, String requestedBy) {
        if (!flags.enabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI job intelligence is currently disabled");
        }
        jobPostingService.getById(jobPostingId); // 404 for unknown posting

        try {
            urlValidator.validate(officialUrl);
        } catch (UrlValidator.InvalidUrlException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        }

        if (runRepository.existsByJobPostingIdAndStatusIn(jobPostingId, ACTIVE_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An AI analysis is already running for this posting");
        }

        JobIntelligenceRun run = new JobIntelligenceRun();
        run.setJobPostingId(jobPostingId);
        run.setOfficialUrl(officialUrl.trim());
        run.setUrlHash(sha256(normalizeUrl(officialUrl)));
        run.setStatus(RunStatus.PENDING);
        run.setRequestedBy(requestedBy);
        JobIntelligenceRun saved = runRepository.save(run);

        eventPublisher.publish(JobIntelligenceRequestedEvent.of(
                saved.getId(), jobPostingId, saved.getOfficialUrl()));
        log.info("JOB_INTEL event=RUN_REQUESTED runId={} postingId={} requestedBy={}",
                saved.getId(), jobPostingId, requestedBy);
        return saved;
    }

    /** Retry a failed run: same run row, reset for reprocessing (skills are never duplicated). */
    @Transactional
    public JobIntelligenceRun retry(UUID runId) {
        if (!flags.enabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI job intelligence is currently disabled");
        }
        JobIntelligenceRun run = getRun(runId);
        if (run.getStatus() != RunStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Only failed runs can be retried");
        }
        run.setStatus(RunStatus.PENDING);
        run.setErrorMessage(null);
        run.setRetryCount(run.getRetryCount() + 1);
        JobIntelligenceRun saved = runRepository.save(run);

        eventPublisher.publish(JobIntelligenceRequestedEvent.of(
                saved.getId(), saved.getJobPostingId(), saved.getOfficialUrl()));
        log.info("JOB_INTEL event=RUN_RETRIED runId={} attempt={}", runId, saved.getRetryCount());
        return saved;
    }

    @Transactional(readOnly = true)
    public JobIntelligenceRun getRun(UUID runId) {
        return runRepository.findById(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found"));
    }

    @Transactional(readOnly = true)
    public JobIntelligenceRun getLatestForPosting(UUID jobPostingId) {
        return runRepository.findTopByJobPostingIdOrderByCreatedAtDesc(jobPostingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No AI analysis exists for this posting"));
    }

    // ── Progress updates (REQUIRES_NEW so pollers see them mid-pipeline) ────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStage(UUID runId, RunStatus status) {
        mutate(runId, run -> {
            run.setStatus(status);
            if (run.getStartedAt() == null) {
                run.setStartedAt(Instant.now());
            }
        });
        log.info("JOB_INTEL event=STAGE runId={} stage={}", runId, status);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyUpdate(UUID runId, Consumer<JobIntelligenceRun> update) {
        mutate(runId, update);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(UUID runId, Consumer<JobIntelligenceRun> finalUpdate) {
        mutate(runId, run -> {
            finalUpdate.accept(run);
            run.setStatus(RunStatus.COMPLETED);
            run.setCompletedAt(Instant.now());
            if (run.getStartedAt() != null) {
                run.setDurationMs(run.getCompletedAt().toEpochMilli()
                        - run.getStartedAt().toEpochMilli());
            }
        });
        log.info("JOB_INTEL event=RUN_COMPLETED runId={}", runId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID runId, String errorMessage) {
        mutate(runId, run -> {
            run.setStatus(RunStatus.FAILED);
            run.setErrorMessage(errorMessage);
            run.setCompletedAt(Instant.now());
            if (run.getStartedAt() != null) {
                run.setDurationMs(run.getCompletedAt().toEpochMilli()
                        - run.getStartedAt().toEpochMilli());
            }
        });
        log.warn("JOB_INTEL event=RUN_FAILED runId={} error={}", runId, errorMessage);
    }

    private void mutate(UUID runId, Consumer<JobIntelligenceRun> update) {
        JobIntelligenceRun run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalStateException("Run disappeared: " + runId));
        update.accept(run);
        runRepository.save(run);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /** Cache-key normalization: scheme/host lower-cased, fragment dropped. */
    public static String normalizeUrl(String url) {
        String trimmed = url.trim();
        int fragment = trimmed.indexOf('#');
        if (fragment >= 0) {
            trimmed = trimmed.substring(0, fragment);
        }
        int schemeEnd = trimmed.indexOf("://");
        if (schemeEnd > 0) {
            int pathStart = trimmed.indexOf('/', schemeEnd + 3);
            String head = pathStart > 0 ? trimmed.substring(0, pathStart) : trimmed;
            String tail = pathStart > 0 ? trimmed.substring(pathStart) : "";
            return head.toLowerCase(Locale.ROOT) + tail;
        }
        return trimmed;
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
