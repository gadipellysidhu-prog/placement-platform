package com.college.placement.modules.student.domain;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.shared.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;


@Entity
@Table(
    name = "students",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_students_user_id",     columnNames = "user_id"),
        @UniqueConstraint(name = "uq_students_roll_number", columnNames = "roll_number")
    },
    indexes = @Index(name = "idx_students_branch_id", columnList = "branch_id")
)
@Getter
@Setter
@NoArgsConstructor
public class Student extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @NotBlank
    @Size(max = 50)
    @Column(name = "roll_number", nullable = false, length = 50)
    private String rollNumber;

    @DecimalMin("0.0")
    @DecimalMax("10.0")
    @Column(precision = 4, scale = 2)
    private BigDecimal cgpa;

    @Min(1)
    @Max(6)
    @Column(name = "current_year", nullable = false)
    private int currentYear = 1;

    @Column(name = "placement_eligible", nullable = false)
    private boolean placementEligible = true;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StudentStatus status = StudentStatus.ACTIVE;

    @ManyToMany(fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    @JoinTable(
        name = "student_skills",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> skills = new LinkedHashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student s)) return false;
        return id != null && id.equals(s.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
