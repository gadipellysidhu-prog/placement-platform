package com.college.placement.modules.company.domain;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.shared.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
    name = "recruiters",
    uniqueConstraints = @UniqueConstraint(name = "uq_recruiters_user_id", columnNames = "user_id"),
    indexes = @Index(name = "idx_recruiters_company_id", columnList = "company_id")
)
@Getter
@Setter
@NoArgsConstructor
public class Recruiter extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Size(max = 100)
    @Column(length = 100)
    private String designation;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Recruiter r)) return false;
        return id != null && id.equals(r.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
