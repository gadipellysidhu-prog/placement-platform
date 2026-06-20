package com.college.placement.modules.aigovernance.domain;

import com.college.placement.shared.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
    name = "inference_history",
    indexes = {
        @Index(name = "idx_inference_history_ai_model_id",  columnList = "ai_model_id"),
        @Index(name = "idx_inference_history_requested_by", columnList = "requested_by")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class InferenceHistory extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_model_id", nullable = false)
    private AIModel aiModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_registry_id")
    private PromptRegistry promptRegistry;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens = 0;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens = 0;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs = 0;

    @Size(max = 255)
    @Column(name = "requested_by", length = 255)
    private String requestedBy;

    @Column(nullable = false)
    private boolean success = true;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InferenceHistory i)) return false;
        return id != null && id.equals(i.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
