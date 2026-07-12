package com.college.placement.modules.jobintelligence.service;

import com.college.placement.modules.aigovernance.service.AIGovernanceService;
import com.college.placement.modules.company.service.JobPostingService;
import com.college.placement.modules.jobintelligence.ai.AIProvider;
import com.college.placement.modules.jobintelligence.ai.PromptBuilder;
import com.college.placement.modules.jobintelligence.configuration.JobIntelligenceFlags;
import com.college.placement.modules.jobintelligence.configuration.JobIntelligenceProperties;
import com.college.placement.modules.jobintelligence.crawler.PageFetcher;
import com.college.placement.modules.jobintelligence.crawler.UrlValidator;
import com.college.placement.modules.jobintelligence.domain.ExtractionCacheEntry;
import com.college.placement.modules.jobintelligence.domain.JobIntelligenceRun;
import com.college.placement.modules.jobintelligence.domain.RunStatus;
import com.college.placement.modules.jobintelligence.dto.ExtractedJobData;
import com.college.placement.modules.jobintelligence.events.JobIntelligenceCompletedEvent;
import com.college.placement.modules.jobintelligence.extractor.HtmlContentExtractor;
import com.college.placement.modules.jobintelligence.metrics.JobIntelligenceMetrics;
import com.college.placement.modules.jobintelligence.repository.ExtractionCacheRepository;
import com.college.placement.modules.jobintelligence.validation.ExtractionValidator;
import com.college.placement.modules.student.domain.Branch;
import com.college.placement.modules.student.domain.Skill;
import com.college.placement.modules.student.domain.SkillCreatedSource;
import com.college.placement.modules.student.service.SkillNormalizationService;
import com.college.placement.modules.student.service.SkillService;
import com.college.placement.shared.eventbus.EventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The AI extraction pipeline orchestrator. Executes entirely on the module's own
 * async executor — never on an HTTP thread. Each stage updates the run row in its
 * own transaction so the frontend sees live progress; per-skill failures become
 * warnings and never abort the run; a pipeline failure only marks the run FAILED
 * and can never affect the posting itself.
 */
@Slf4j
@Service
public class JobIntelligencePipeline {

    private final JobIntelligenceService runService;
    private final PageFetcher pageFetcher;
    private final HtmlContentExtractor contentExtractor;
    private final PromptBuilder promptBuilder;
    private final AIProvider aiProvider;
    private final ExtractionValidator extractionValidator;
    private final ExtractionCacheRepository cacheRepository;
    private final SkillNormalizationService normalizationService;
    private final SkillService skillService;
    private final JobPostingService jobPostingService;
    private final BranchPredictionService branchPredictionService;
    private final JobIntelligenceFlags flags;
    private final JobIntelligenceProperties properties;
    private final JobIntelligenceMetrics metrics;
    private final AIGovernanceService aiGovernanceService;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public JobIntelligencePipeline(JobIntelligenceService runService,
                                   PageFetcher pageFetcher,
                                   HtmlContentExtractor contentExtractor,
                                   PromptBuilder promptBuilder,
                                   AIProvider aiProvider,
                                   ExtractionValidator extractionValidator,
                                   ExtractionCacheRepository cacheRepository,
                                   SkillNormalizationService normalizationService,
                                   SkillService skillService,
                                   JobPostingService jobPostingService,
                                   BranchPredictionService branchPredictionService,
                                   JobIntelligenceFlags flags,
                                   JobIntelligenceProperties properties,
                                   JobIntelligenceMetrics metrics,
                                   AIGovernanceService aiGovernanceService,
                                   EventPublisher eventPublisher,
                                   ObjectMapper objectMapper) {
        this.runService = runService;
        this.pageFetcher = pageFetcher;
        this.contentExtractor = contentExtractor;
        this.promptBuilder = promptBuilder;
        this.aiProvider = aiProvider;
        this.extractionValidator = extractionValidator;
        this.cacheRepository = cacheRepository;
        this.normalizationService = normalizationService;
        this.skillService = skillService;
        this.jobPostingService = jobPostingService;
        this.branchPredictionService = branchPredictionService;
        this.flags = flags;
        this.properties = properties;
        this.metrics = metrics;
        this.aiGovernanceService = aiGovernanceService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    /** Execute the full pipeline for a run. Never throws — failures land on the run row. */
    public void execute(UUID runId) {
        Instant pipelineStart = Instant.now();
        metrics.runStarted();
        JobIntelligenceRun run = runService.getRun(runId);
        List<String> warnings = new ArrayList<>();
        try {
            // 1-2. Fetch + extract structured data (or reuse cached extraction).
            ExtractedJobData data = obtainStructuredData(run, warnings);

            // 3. Normalize + catalog match/update + tag the posting.
            runService.updateStage(runId, RunStatus.NORMALIZING);
            List<String> mentions = data.allSkillMentions();
            metrics.skillsExtracted(mentions.size());
            TagOutcome tagOutcome = normalizeAndTag(run, mentions, warnings);

            // 4. Branch prediction.
            List<String> predictedNames = List.of();
            if (flags.branchPrediction()) {
                runService.updateStage(runId, RunStatus.PREDICTING_BRANCHES);
                predictedNames = predictAndAttachBranches(run, mentions, warnings);
            }

            // 5. Finalize the run row.
            String extractedJson = objectMapper.writeValueAsString(data);
            String warningsJson = objectMapper.writeValueAsString(warnings);
            BigDecimal confidence = data.confidence() == null
                    ? null : BigDecimal.valueOf(data.confidence());
            List<String> finalPredicted = predictedNames;
            runService.markCompleted(runId, r -> {
                r.setProvider(aiProvider.id());
                r.setModel(aiProvider.model());
                r.setConfidence(confidence);
                r.setSkillsExtracted(mentions.size());
                r.setSkillsCreated(tagOutcome.created());
                r.setSkillsTagged(tagOutcome.tagged());
                r.setPredictedBranches(String.join(", ", finalPredicted));
                r.setExtractedJson(extractedJson);
                r.setWarningsJson(warningsJson);
            });
            metrics.runCompleted();
            metrics.recordExtractionDuration(Duration.between(pipelineStart, Instant.now()));
            publishCompletion(run, true, tagOutcome.tagged());
        } catch (Exception ex) {
            metrics.runFailed();
            runService.markFailed(runId, failureMessage(ex));
            publishCompletion(run, false, 0);
        }
    }

    // ── Stage: fetch + LLM extraction (with cache) ──────────────────────────

    private ExtractedJobData obtainStructuredData(JobIntelligenceRun run, List<String> warnings)
            throws Exception {
        Optional<ExtractionCacheEntry> cached = cacheRepository.findByUrlHash(run.getUrlHash())
                .filter(entry -> entry.getExpiresAt().isAfter(Instant.now()));
        if (cached.isPresent()) {
            metrics.cacheHit();
            log.info("JOB_INTEL event=CACHE_HIT runId={} urlHash={}", run.getId(), run.getUrlHash());
            runService.updateStage(run.getId(), RunStatus.EXTRACTING);
            return extractionValidator.validate(cached.get().getStructuredJson());
        }
        metrics.cacheMiss();

        runService.updateStage(run.getId(), RunStatus.FETCHING);
        String html = fetchWithRetries(run.getOfficialUrl());
        String visibleText = contentExtractor.extractVisibleText(html);
        if (visibleText.isBlank()) {
            throw new ExtractionValidator.InvalidExtractionException(
                    "The page contains no readable text");
        }

        runService.updateStage(run.getId(), RunStatus.EXTRACTING);
        String prompt = promptBuilder.buildExtractionPrompt(visibleText);
        AIProvider.CompletionResult completion = completeWithRetries(prompt, run);
        ExtractedJobData data = extractionValidator.validate(completion.text());

        // Cache the validated (sanitized) extraction for future runs of this URL.
        try {
            String sanitizedJson = objectMapper.writeValueAsString(data);
            ExtractionCacheEntry entry = cacheRepository.findByUrlHash(run.getUrlHash())
                    .orElseGet(ExtractionCacheEntry::new);
            entry.setUrlHash(run.getUrlHash());
            entry.setUrl(run.getOfficialUrl());
            entry.setStructuredJson(sanitizedJson);
            entry.setProvider(aiProvider.id());
            entry.setModel(aiProvider.model());
            entry.setExpiresAt(Instant.now().plus(properties.cache().ttl()));
            cacheRepository.save(entry);
        } catch (Exception ex) {
            warnings.add("Extraction cache write failed: " + ex.getMessage());
        }
        return data;
    }

    private String fetchWithRetries(String url) throws InterruptedException {
        int attempts = 0;
        while (true) {
            try {
                return pageFetcher.fetch(url);
            } catch (PageFetcher.FetchException ex) {
                if (!ex.isTransient() || ++attempts > properties.ai().maxTransientRetries()) {
                    throw ex;
                }
                log.info("JOB_INTEL event=FETCH_RETRY attempt={} reason={}", attempts, ex.getMessage());
                Thread.sleep(Duration.ofSeconds(2L * attempts).toMillis());
            }
        }
    }

    private AIProvider.CompletionResult completeWithRetries(String prompt, JobIntelligenceRun run)
            throws InterruptedException {
        int attempts = 0;
        while (true) {
            long start = System.currentTimeMillis();
            try {
                AIProvider.CompletionResult result = aiProvider.complete(prompt);
                metrics.recordProviderLatency(result.latencyMs());
                recordInference(run, result.inputTokens(), result.outputTokens(),
                        result.latencyMs(), true, null);
                return result;
            } catch (AIProvider.AIProviderException ex) {
                metrics.providerFailure();
                recordInference(run, 0, 0, System.currentTimeMillis() - start, false, ex.getMessage());
                if (!ex.isTransient() || ++attempts > properties.ai().maxTransientRetries()) {
                    throw ex;
                }
                log.info("JOB_INTEL event=LLM_RETRY attempt={} reason={}", attempts, ex.getMessage());
                Thread.sleep(Duration.ofSeconds(3L * attempts).toMillis());
            }
        }
    }

    /** Governance ledger — best-effort; accounting failures never break the pipeline. */
    private void recordInference(JobIntelligenceRun run, int inputTokens, int outputTokens,
                                 long latencyMs, boolean success, String errorMessage) {
        try {
            var model = aiGovernanceService.ensureModel(
                    "job-intelligence:" + aiProvider.id(), aiProvider.id(), aiProvider.model());
            aiGovernanceService.recordInference(model.getId(), null, inputTokens, outputTokens,
                    latencyMs, run.getRequestedBy(), success, errorMessage);
        } catch (Exception ex) {
            log.warn("JOB_INTEL event=GOVERNANCE_RECORD_FAILED error={}", ex.getMessage());
        }
    }

    // ── Stage: normalization, catalog evolution, tagging ────────────────────

    private TagOutcome normalizeAndTag(JobIntelligenceRun run, List<String> mentions,
                                       List<String> warnings) {
        // Resolve every mention to a canonical skill, creating catalog entries for
        // genuinely new skills when allowed. LinkedHashMap dedupes by skill id.
        Map<UUID, Skill> resolved = new LinkedHashMap<>();
        int created = 0;
        for (String mention : mentions) {
            try {
                Optional<Skill> existing = normalizationService.resolve(mention);
                if (existing.isPresent()) {
                    resolved.putIfAbsent(existing.get().getId(), existing.get());
                    continue;
                }
                if (flags.catalogUpdates()) {
                    var outcome = skillService.findOrCreate(mention, null,
                            SkillCreatedSource.AI, confidenceOf(run));
                    if (outcome.created()) {
                        created++;
                        log.info("JOB_INTEL event=CATALOG_SKILL_CREATED name={}", outcome.skill().getName());
                    }
                    resolved.putIfAbsent(outcome.skill().getId(), outcome.skill());
                } else {
                    warnings.add("Unknown skill skipped (catalog updates disabled): " + mention);
                }
            } catch (Exception ex) {
                // Per-skill failures degrade to warnings — the rest of the run continues.
                warnings.add("Skill '" + mention + "' failed: " + ex.getMessage());
                log.warn("JOB_INTEL event=SKILL_FAILED mention={} error={}", mention, ex.getMessage());
            }
        }
        metrics.skillsCreated(created);

        int tagged = 0;
        if (flags.autoTagging()) {
            runService.updateStage(run.getId(), RunStatus.TAGGING);
            for (Skill skill : resolved.values()) {
                try {
                    // Sanctioned business path — same service the manual UI uses; set-add
                    // semantics make retries idempotent.
                    jobPostingService.addRequiredSkill(run.getJobPostingId(), skill.getId());
                    skillService.incrementPopularity(skill.getId());
                    tagged++;
                } catch (Exception ex) {
                    warnings.add("Tagging '" + skill.getName() + "' failed: " + ex.getMessage());
                }
            }
            metrics.skillsTagged(tagged);
        } else {
            warnings.add("Automatic tagging disabled — " + resolved.size() + " skills left unattached");
        }
        log.info("JOB_INTEL event=NORMALIZATION_COMPLETED runId={} mentions={} resolved={} created={} tagged={}",
                run.getId(), mentions.size(), resolved.size(), created, tagged);
        return new TagOutcome(created, tagged);
    }

    private List<String> predictAndAttachBranches(JobIntelligenceRun run, List<String> mentions,
                                                  List<String> warnings) {
        try {
            BranchPredictionService.Prediction prediction = branchPredictionService.predict(mentions);
            for (Branch branch : prediction.matchedBranches()) {
                try {
                    jobPostingService.addEligibleBranch(run.getJobPostingId(), branch.getId());
                } catch (Exception ex) {
                    warnings.add("Branch '" + branch.getName() + "' failed: " + ex.getMessage());
                }
            }
            return prediction.predictedNames();
        } catch (Exception ex) {
            warnings.add("Branch prediction failed: " + ex.getMessage());
            return List.of();
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static BigDecimal confidenceOf(JobIntelligenceRun run) {
        return run.getConfidence();
    }

    private void publishCompletion(JobIntelligenceRun run, boolean success, int skillsTagged) {
        try {
            String title = jobPostingService.getById(run.getJobPostingId()).getTitle();
            eventPublisher.publish(JobIntelligenceCompletedEvent.of(
                    run.getId(), run.getJobPostingId(), title, success, skillsTagged,
                    run.getRequestedBy()));
        } catch (Exception ex) {
            log.warn("JOB_INTEL event=COMPLETION_EVENT_FAILED runId={} error={}",
                    run.getId(), ex.getMessage());
        }
    }

    private static String failureMessage(Exception ex) {
        String message = ex.getMessage();
        return (message == null || message.isBlank()) ? ex.getClass().getSimpleName() : message;
    }

    private record TagOutcome(int created, int tagged) {}
}
