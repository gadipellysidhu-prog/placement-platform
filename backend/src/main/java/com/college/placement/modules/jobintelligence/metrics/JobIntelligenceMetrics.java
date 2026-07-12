package com.college.placement.modules.jobintelligence.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Micrometer metrics for the AI extraction pipeline (mirrors FilePipelineMetrics style). */
@Component
public class JobIntelligenceMetrics {

    private final Counter runsStarted;
    private final Counter runsCompleted;
    private final Counter runsFailed;
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Counter skillsExtracted;
    private final Counter skillsCreated;
    private final Counter skillsTagged;
    private final Counter providerFailures;
    private final Timer extractionTimer;
    private final Timer providerLatency;

    public JobIntelligenceMetrics(MeterRegistry registry) {
        this.runsStarted = Counter.builder("job.intelligence.runs.started")
                .description("AI extraction runs started").register(registry);
        this.runsCompleted = Counter.builder("job.intelligence.runs.completed")
                .description("AI extraction runs completed successfully").register(registry);
        this.runsFailed = Counter.builder("job.intelligence.runs.failed")
                .description("AI extraction runs that ended in failure").register(registry);
        this.cacheHits = Counter.builder("job.intelligence.cache.hits")
                .description("Extraction cache hits").register(registry);
        this.cacheMisses = Counter.builder("job.intelligence.cache.misses")
                .description("Extraction cache misses").register(registry);
        this.skillsExtracted = Counter.builder("job.intelligence.skills.extracted")
                .description("Skill mentions extracted by the LLM").register(registry);
        this.skillsCreated = Counter.builder("job.intelligence.skills.created")
                .description("New catalog skills created by the pipeline").register(registry);
        this.skillsTagged = Counter.builder("job.intelligence.skills.tagged")
                .description("Skills attached to postings by the pipeline").register(registry);
        this.providerFailures = Counter.builder("job.intelligence.provider.failures")
                .description("LLM provider call failures").register(registry);
        this.extractionTimer = Timer.builder("job.intelligence.extraction.duration")
                .description("End-to-end pipeline duration").register(registry);
        this.providerLatency = Timer.builder("job.intelligence.provider.latency")
                .description("LLM provider call latency").register(registry);
    }

    public void runStarted() { runsStarted.increment(); }
    public void runCompleted() { runsCompleted.increment(); }
    public void runFailed() { runsFailed.increment(); }
    public void cacheHit() { cacheHits.increment(); }
    public void cacheMiss() { cacheMisses.increment(); }
    public void skillsExtracted(int count) { skillsExtracted.increment(count); }
    public void skillsCreated(int count) { skillsCreated.increment(count); }
    public void skillsTagged(int count) { skillsTagged.increment(count); }
    public void providerFailure() { providerFailures.increment(); }
    public void recordExtractionDuration(Duration duration) { extractionTimer.record(duration); }
    public void recordProviderLatency(long millis) { providerLatency.record(Duration.ofMillis(millis)); }
}
