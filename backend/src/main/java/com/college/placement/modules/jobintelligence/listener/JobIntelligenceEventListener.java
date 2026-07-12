package com.college.placement.modules.jobintelligence.listener;

import com.college.placement.modules.jobintelligence.configuration.JobIntelligenceFlags;
import com.college.placement.modules.jobintelligence.domain.RunStatus;
import com.college.placement.modules.jobintelligence.events.JobIntelligenceRequestedEvent;
import com.college.placement.modules.jobintelligence.repository.JobIntelligenceRunRepository;
import com.college.placement.modules.jobintelligence.service.JobIntelligencePipeline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges the domain event to the pipeline. Fires AFTER_COMMIT (the run row is
 * guaranteed visible) and hops onto the module's dedicated executor so LLM/network
 * latency can never block HTTP threads or starve the shared event-bus pool.
 */
@Slf4j
@Component
public class JobIntelligenceEventListener {

    private final JobIntelligencePipeline pipeline;
    private final JobIntelligenceRunRepository runRepository;
    private final JobIntelligenceFlags flags;

    public JobIntelligenceEventListener(JobIntelligencePipeline pipeline,
                                        JobIntelligenceRunRepository runRepository,
                                        JobIntelligenceFlags flags) {
        this.pipeline = pipeline;
        this.runRepository = runRepository;
        this.flags = flags;
    }

    @Async("jobIntelligenceExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRequested(JobIntelligenceRequestedEvent event) {
        if (!flags.enabled()) {
            log.info("JOB_INTEL event=SKIPPED reason=disabled runId={}", event.runId());
            return;
        }
        // Idempotency: the sweeper or duplicate events may deliver twice; only a
        // PENDING run may enter the pipeline.
        boolean pending = runRepository.findById(event.runId())
                .map(run -> run.getStatus() == RunStatus.PENDING)
                .orElse(false);
        if (!pending) {
            log.info("JOB_INTEL event=SKIPPED reason=not_pending runId={}", event.runId());
            return;
        }
        pipeline.execute(event.runId());
    }
}
