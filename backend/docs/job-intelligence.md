# Job Intelligence Module — Developer Guide

The AI Job Intelligence System analyzes a job posting's **official URL**, extracts
structured data with an LLM, normalizes skills against the **Master Skills Catalog**,
auto-tags the posting through existing business services, and predicts eligible
branches. The officer reviews; the AI never locks anything.

## Architecture overview

```
Officer → POST /api/job-intelligence/runs         (202, returns run id)
            └─ JobIntelligenceService.startRun    (run row + JobIntelligenceRequestedEvent)
                       │ AFTER_COMMIT, @Async("jobIntelligenceExecutor")
                       ▼
            JobIntelligenceEventListener → JobIntelligencePipeline.execute(runId)
              1. FETCHING    PageFetcher (UrlValidator SSRF guard, manual redirects, 2MB cap)
              2. EXTRACTING  HtmlContentExtractor (jsoup boilerplate strip)
                             PromptBuilder → AIProvider → ExtractionValidator (trust boundary)
                             ExtractionCacheEntry per URL hash (TTL, reused on re-runs)
              3. NORMALIZING SkillNormalizationService.resolve → SkillService.findOrCreate(AI)
              4. TAGGING     JobPostingService.addRequiredSkill  (sanctioned path, idempotent)
              5. PREDICTING  BranchPredictionService → addEligibleBranch
              6. COMPLETED/FAILED on the run row + JobIntelligenceCompletedEvent
                             └─ outbox → NotificationOutboxHandler → officer email
```

Progress is written per-stage with `REQUIRES_NEW` transactions, so the frontend can
poll `GET /api/job-intelligence/runs/{id}` and see the live stage.

## Event flow & durability

- `JobIntelligenceRequestedEvent` triggers the pipeline (AFTER_COMMIT + async).
- Every DomainEvent is also captured by the transactional outbox automatically.
- Crash recovery: `RunSweeper` re-publishes runs stuck in `PENDING` beyond
  `job.intelligence.sweeper.stuck-after` and evicts expired cache rows.
- Retry semantics: transient failures (timeout / 429 / 5xx) retry in-pipeline with
  backoff (`max-transient-retries`); permanent failures (invalid URL, 404, malformed
  LLM JSON) fail immediately. `POST /runs/{id}/retry` re-enters the same run row —
  set-add tagging keeps retries idempotent (no duplicate skills).

## AI provider architecture

`AIProvider` is the port; the active implementation is chosen by
`job.intelligence.ai.provider`:

| value | implementation | notes |
|---|---|---|
| `stub` (default) | `StubAIProvider` | deterministic keyword scan, offline, used by tests |
| `openai-compatible` / `groq` / `ollama` / `openai` | `OpenAICompatibleProvider` | any `/chat/completions` endpoint |

Example — Groq hosting an open-source model:

```bash
JOB_INTEL_AI_PROVIDER=openai-compatible
JOB_INTEL_AI_BASE_URL=https://api.groq.com/openai/v1
JOB_INTEL_AI_MODEL=llama-3.3-70b-versatile
JOB_INTEL_AI_API_KEY=...           # environment only, never in code/config files
```

Local Ollama: `base-url=http://localhost:11434/v1`, no key (requires
`allow-private-networks=true` in dev).

Every call is recorded in the aigovernance ledger
(`AIGovernanceService.ensureModel` + `recordInference`): tokens, latency, success.

**Adding a new provider**: implement `AIProvider`, add a case in
`JobIntelligenceConfig.aiProvider`, select it via configuration. Nothing else changes.

## Prompt design & AI-output trust boundary

- `PromptBuilder` sends only boilerplate-stripped visible text (never raw HTML),
  prefixed with an anti-prompt-injection preamble (page text is untrusted data),
  and demands the exact JSON schema.
- `ExtractionValidator` is the single trust boundary: strict JSON parsing (code
  fences tolerated), `requiredSkills` mandatory, arrays capped at 100 entries,
  strings length-capped, and **every string stripped of HTML** before persistence.
  Malformed output = non-retryable failure.

## Skill normalization & catalog evolution

Resolution chain (`SkillNormalizationService`): exact name → alias → abbreviation
map → fuzzy (pg_trgm in prod, Levenshtein under the H2 test profile). Unknown
skills become catalog entries via `SkillService.findOrCreate` with
`created_source=AI` + auto-generated name-variant aliases — the catalog improves
with every analyzed posting, and duplicates are impossible through any path.

## Configuration

Static (`application.yml`, `job.intelligence.*`, env-overridable `JOB_INTEL_*`):
crawler timeouts/size/redirects, `crawler.allow-private-networks` (**tests only**),
AI provider settings, cache TTL, sweeper cadence.

Runtime kill-switches (SettingsService keys, changeable without restart via
`/api/settings`):

| key | default | effect when off |
|---|---|---|
| `job.intelligence.enabled` | `false` | runs rejected (503); listener/sweeper no-op — full manual workflow |
| `job.intelligence.auto-tagging` | `true` | skills resolved but not attached |
| `job.intelligence.catalog-updates` | `true` | unknown skills skipped (warning) |
| `job.intelligence.branch-prediction` | `true` | branch stage skipped |

Rollback = flip `job.intelligence.enabled` to `false`. No schema rollback needed.

## Security

- SSRF: http/https only, no credentials in URL, every DNS answer checked against
  loopback/RFC1918/link-local/CGNAT/metadata ranges, every redirect re-validated.
- Downloaded pages are parsed, never executed; 2 MB body cap.
- LLM output fully sanitized (see trust boundary). API keys only via environment.

## Observability

Micrometer (`job.intelligence.*`): runs started/completed/failed, stage/extraction
timers, provider latency + failures, skills extracted/created/tagged, cache
hits/misses. Structured logs use `JOB_INTEL event=<STAGE|RUN_FAILED|CACHE_HIT|…>
key=value`. Inference history is queryable through the aigovernance repositories.

## Troubleshooting

| symptom | check |
|---|---|
| run stuck in PENDING | listener executor alive? sweeper will requeue after `stuck-after` |
| 503 on start | `job.intelligence.enabled` setting |
| FAILED: "resolves to a non-public address" | SSRF guard — the URL points at an internal IP |
| FAILED: "not valid JSON" | provider/model mismatch; check the model supports JSON output |
| nothing tagged but COMPLETED | `auto-tagging` flag off, or warnings on the run row |
| repeated LLM calls for same URL | cache TTL expired or URL differs (hash is normalized-URL based) |
