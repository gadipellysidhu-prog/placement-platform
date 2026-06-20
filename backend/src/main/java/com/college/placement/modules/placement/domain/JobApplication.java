package com.college.placement.modules.placement.domain;

import com.college.placement.modules.company.domain.JobPosting;
import com.college.placement.modules.student.domain.Student;
import com.college.placement.shared.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "job_applications",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_job_applications_student_posting",
        columnNames = {"student_id", "job_posting_id"}
    ),
    indexes = {
        @Index(name = "idx_job_applications_student_id",     columnList = "student_id"),
        @Index(name = "idx_job_applications_job_posting_id", columnList = "job_posting_id"),
        @Index(name = "idx_job_applications_status",         columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class JobApplication extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_posting_id", nullable = false)
    private JobPosting jobPosting;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(name = "applied_at", nullable = false, updatable = false)
    private Instant appliedAt;

    @PrePersist
    void prePersist() {
        appliedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobApplication a)) return false;
        return id != null && id.equals(a.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
