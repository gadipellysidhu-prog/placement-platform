package com.college.placement.modules.company.domain;

import com.college.placement.modules.student.domain.Branch;
import com.college.placement.modules.student.domain.Skill;
import com.college.placement.shared.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
    name = "job_postings",
    indexes = {
        @Index(name = "idx_job_postings_company_id", columnList = "company_id"),
        @Index(name = "idx_job_postings_status",     columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class JobPosting extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @DecimalMin("0.0")
    @Column(name = "ctc_min", precision = 15, scale = 2)
    private BigDecimal ctcMin;

    @DecimalMin("0.0")
    @Column(name = "ctc_max", precision = 15, scale = 2)
    private BigDecimal ctcMax;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private JobPostingStatus status = JobPostingStatus.DRAFT;

    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;

    @Min(1)
    @Column(name = "offer_limit", nullable = false)
    private int offerLimit = 1;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "job_posting_skills",
        joinColumns = @JoinColumn(name = "job_posting_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> requiredSkills = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "job_posting_branches",
        joinColumns = @JoinColumn(name = "job_posting_id"),
        inverseJoinColumns = @JoinColumn(name = "branch_id")
    )
    private Set<Branch> eligibleBranches = new LinkedHashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobPosting j)) return false;
        return id != null && id.equals(j.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
