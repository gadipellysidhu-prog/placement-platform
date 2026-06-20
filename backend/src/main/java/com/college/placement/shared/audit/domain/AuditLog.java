package com.college.placement.shared.audit.domain;

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
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_logs_entity",       columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_logs_performed_by", columnList = "performed_by")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class AuditLog extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @NotBlank
    @Size(max = 100)
    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String action;

    @Size(max = 255)
    @Column(name = "performed_by", length = 255)
    private String performedBy;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditLog a)) return false;
        return id != null && id.equals(a.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
