package com.college.placement.modules.jobintelligence.domain;

import com.college.placement.shared.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Cached validated AI output for an official job URL. Re-processing the same URL
 * within the TTL reuses this instead of re-crawling and re-invoking the LLM.
 */
@Entity
@Table(
    name = "job_extraction_cache",
    uniqueConstraints = @UniqueConstraint(name = "uq_job_extraction_cache_url_hash", columnNames = "url_hash"),
    indexes = @Index(name = "idx_job_extraction_cache_expires", columnList = "expires_at")
)
@Getter
@Setter
@NoArgsConstructor
public class ExtractionCacheEntry extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 64)
    @Column(name = "url_hash", nullable = false, length = 64)
    private String urlHash;

    @NotBlank
    @Size(max = 2048)
    @Column(nullable = false, length = 2048)
    private String url;

    @NotBlank
    @Column(name = "structured_json", nullable = false, columnDefinition = "TEXT")
    private String structuredJson;

    @Size(max = 100)
    @Column(length = 100)
    private String provider;

    @Size(max = 100)
    @Column(length = 100)
    private String model;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExtractionCacheEntry e)) return false;
        return id != null && id.equals(e.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
