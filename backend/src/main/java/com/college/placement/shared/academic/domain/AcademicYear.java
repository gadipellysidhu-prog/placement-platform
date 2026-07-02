package com.college.placement.shared.academic.domain;

import com.college.placement.shared.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * An academic year / placement season (e.g. {@code 2025-2026}). Acts as the
 * versioning key for placement policies and the partitioning dimension for
 * analytics snapshots. At most one academic year is {@code active} at a time,
 * enforced by a partial unique index.
 */
@Entity
@Table(
    name = "academic_years",
    indexes = @Index(name = "idx_academic_years_label", columnList = "label")
)
@Getter
@Setter
@NoArgsConstructor
public class AcademicYear extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String label;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean active = false;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AcademicYear a)) return false;
        return id != null && id.equals(a.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
