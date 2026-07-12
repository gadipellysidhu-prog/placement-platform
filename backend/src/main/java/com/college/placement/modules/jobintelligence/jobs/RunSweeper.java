package com.college.placement.modules.jobintelligence.jobs;

import com.college.placement.modules.jobintelligence.configuration.JobIntelligenceFlags;
import com.college.placement.modules.jobintelligence.configuration.JobIntelligenceProperties;
import com.college.placement.modules.jobintelligence.domain.JobIntelligenceRun;
import com.college.placement.modules.jobintelligence.domain.RunStatus;
import com.college.placement.modules.jobintelligence.events.JobIntelligenceRequestedEvent;
import com.college.placement.modules.jobintelligence.repository.ExtractionCacheRepository;
import com.college.placement.modules.jobintelligence.repository.JobIntelligenceRunRepository;
import com.college.placement.shared.eventbus.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Housekeeping: re-queues runs stuck in PENDING (e.g. the process died before the
 * async listener ran) and evicts expired extraction-cache rows. Never touches
 * in-flight or terminal runs, and never modifies job postings.
 */
@Slf4j
@Component
public class RunSweeper {

    private final JobIntelligenceRunRepository runRepository;
    private final ExtractionCacheRepository cacheRepository;
    private final JobIntelligenceProperties properties;
    private final JobIntelligenceFlags flags;
    private final EventPublisher eventPublisher;

    public RunSweeper(JobIntelligenceRunRepository runRepository,
                      ExtractionCacheRepository cacheRepository,
                      JobIntelligenceProperties properties,
                      JobIntelligenceFlags flags,
                      EventPublisher eventPublisher) {
        this.runRepository = runRepository;
        this.cacheRepository = cacheRepository;
        this.properties = properties;
        this.flags = flags;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(initialDelayString = "${job.intelligence.sweeper.initial-delay-ms:60000}",
               fixedDelayString = "${job.intelligence.sweeper.fixed-delay-ms:300000}")
    @Transactional
    public void sweep() {
        if (!flags.enabled()) {
            return;
        }
        Instant cutoff = Instant.now().minus(properties.sweeper().stuckAfter());
        List<JobIntelligenceRun> stuck =
                runRepository.findByStatusAndCreatedAtBefore(RunStatus.PENDING, cutoff);
        for (JobIntelligenceRun run : stuck) {
            log.info("JOB_INTEL event=SWEEPER_REQUEUE runId={} pendingSince={}",
                    run.getId(), run.getCreatedAt());
            eventPublisher.publish(JobIntelligenceRequestedEvent.of(
                    run.getId(), run.getJobPostingId(), run.getOfficialUrl()));
        }

        int evicted = cacheRepository.deleteExpired(Instant.now());
        if (evicted > 0) {
            log.info("JOB_INTEL event=CACHE_EVICTED count={}", evicted);
        }
    }
}
