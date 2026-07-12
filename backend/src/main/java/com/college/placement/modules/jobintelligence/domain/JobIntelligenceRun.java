package com.college.placement.modules.jobintelligence.domain;

import com.college.placement.shared.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One AI extraction run for a job posting's official URL. The run row is the
 * single source of truth the frontend polls for live progress, and the audit
 * trail of what the pipeline did (counts, provider, warnings, errors, timing).
 *
 * <p>The official URL deliberately lives here — not on {@code JobPosting} — so
 * the existing posting entity/DTO contracts stay untouched.
 */
@Entity
@Table(
    name = "job_intelligence_runs",
    indexes = {
        @Index(name = "idx_job_intel_runs_posting", columnList = "job_posting_id, created_at"),
        @Index(name = "idx_job_intel_runs_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class JobIntelligenceRun extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(name = "job_posting_id", nullable = false)
    private UUID jobPostingId;

    @NotBlank
    @Size(max = 2048)
    @Column(name = "official_url", nullable = false, length = 2048)
    private String officialUrl;

    /** SHA-256 hex of the normalized URL — cache key. */
    @NotBlank
    @Size(max = 64)
    @Column(name = "url_hash", nullable = false, length = 64)
    private String urlHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RunStatus status = RunStatus.PENDING;

    @Size(max = 100)
    @Column(length = 100)
    private String provider;

    @Size(max = 100)
    @Column(length = 100)
    private String model;

    /** Overall extraction confidence 0–100 reported by the model, when available. */
    @Column(precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(name = "skills_extracted", nullable = false)
    private int skillsExtracted = 0;

    @Column(name = "skills_created", nullable = false)
    private int skillsCreated = 0;

    @Column(name = "skills_tagged", nullable = false)
    private int skillsTagged = 0;

    /** Comma-separated predicted branch names shown to the officer. */
    @Size(max = 500)
    @Column(name = "predicted_branches", length = 500)
    private String predictedBranches;

    /** Validated structured LLM output (sanitized JSON). */
    @Column(name = "extracted_json", columnDefinition = "TEXT")
    private String extractedJson;

    /** JSON array of non-fatal warnings collected during the run. */
    @Column(name = "warnings_json", columnDefinition = "TEXT")
    private String warningsJson;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Size(max = 255)
    @Column(name = "requested_by", length = 255)
    private String requestedBy;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobIntelligenceRun r)) return false;
        return id != null && id.equals(r.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
