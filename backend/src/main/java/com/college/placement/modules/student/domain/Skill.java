package com.college.placement.modules.student.domain;

import com.college.placement.shared.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
    name = "skills",
    uniqueConstraints = @UniqueConstraint(name = "uq_skills_name", columnNames = "name"),
    indexes = @Index(name = "idx_skills_name", columnList = "name")
)
@Getter
@Setter
@NoArgsConstructor
public class Skill extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @Size(max = 50)
    @Column(length = 50)
    private String category;

    @Column(nullable = false)
    private boolean verified = false;

    // ── Master Skills Catalog metadata (V19_0_0) — all optional/defaulted ──

    @Column(columnDefinition = "TEXT")
    private String description;

    @Size(max = 50)
    @Column(name = "parent_category", length = 50)
    private String parentCategory;

    @Size(max = 50)
    @Column(length = 50)
    private String subcategory;

    /** Usage-frequency indicator; bumped whenever the skill is tagged onto a posting. */
    @Column(name = "popularity_score", nullable = false)
    private int popularityScore = 0;

    @Size(max = 255)
    @Column(name = "industry_tags", length = 255)
    private String industryTags;

    /** Inactive skills are hidden from search/suggestions but keep historical references. */
    @Column(nullable = false)
    private boolean active = true;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "created_source", nullable = false, length = 20)
    private SkillCreatedSource createdSource = SkillCreatedSource.MANUAL;

    /** 0–100 confidence reported by the AI pipeline; null for non-AI skills. */
    @Column(name = "ai_confidence", precision = 5, scale = 2)
    private BigDecimal aiConfidence;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Skill s)) return false;
        return id != null && id.equals(s.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
