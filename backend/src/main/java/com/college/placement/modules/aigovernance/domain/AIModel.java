package com.college.placement.modules.aigovernance.domain;

import com.college.placement.shared.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
    name = "ai_models",
    uniqueConstraints = @UniqueConstraint(name = "uq_ai_models_name", columnNames = "name")
)
@Getter
@Setter
@NoArgsConstructor
public class AIModel extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String provider;

    @Size(max = 100)
    @Column(name = "model_version", length = 100)
    private String modelVersion;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AIModel m)) return false;
        return id != null && id.equals(m.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
