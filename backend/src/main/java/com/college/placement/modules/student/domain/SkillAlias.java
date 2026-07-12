package com.college.placement.modules.student.domain;

import com.college.placement.shared.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.UUID;

/**
 * Alternative name/abbreviation for a catalog {@link Skill} (e.g. "ReactJS" → React).
 * {@code aliasNormalized} (lower/trim/space-collapsed) is globally unique so an alias
 * can never resolve to two different skills.
 */
@Entity
@Table(
    name = "skill_aliases",
    uniqueConstraints = @UniqueConstraint(name = "uq_skill_aliases_normalized", columnNames = "alias_normalized"),
    indexes = @Index(name = "idx_skill_aliases_skill_id", columnList = "skill_id")
)
@Getter
@Setter
@NoArgsConstructor
public class SkillAlias extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Skill skill;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String alias;

    @NotBlank
    @Size(max = 100)
    @Column(name = "alias_normalized", nullable = false, length = 100)
    private String aliasNormalized;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SkillAlias a)) return false;
        return id != null && id.equals(a.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
