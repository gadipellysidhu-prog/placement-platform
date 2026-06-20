package com.college.placement.modules.aigovernance.domain;

import com.college.placement.shared.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
    name = "prompt_registry",
    uniqueConstraints = @UniqueConstraint(name = "uq_prompt_registry_key", columnNames = "prompt_key"),
    indexes = @Index(name = "idx_prompt_registry_ai_model_id", columnList = "ai_model_id")
)
@Getter
@Setter
@NoArgsConstructor
public class PromptRegistry extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_model_id", nullable = false)
    private AIModel aiModel;

    @NotBlank
    @Size(max = 100)
    @Column(name = "prompt_key", nullable = false, length = 100, unique = true)
    private String promptKey;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String template;

    @Min(1)
    @Column(name = "prompt_version", nullable = false)
    private int promptVersion = 1;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PromptRegistry p)) return false;
        return id != null && id.equals(p.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
