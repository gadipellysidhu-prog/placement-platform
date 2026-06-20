package com.college.placement.modules.certificate.domain;

import com.college.placement.modules.student.domain.Skill;
import com.college.placement.modules.student.domain.Student;
import com.college.placement.shared.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
    name = "certificates",
    indexes = @Index(name = "idx_certificates_student_id", columnList = "student_id")
)
@Getter
@Setter
@NoArgsConstructor
public class Certificate extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Size(max = 255)
    @Column(nullable = false)
    private String name;

    @Size(max = 255)
    @Column(name = "issuing_organization", length = 255)
    private String issuingOrganization;

    @Size(max = 500)
    @Column(name = "file_key", length = 500)
    private String fileKey;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 50)
    private CertificateVerificationStatus verificationStatus = CertificateVerificationStatus.PENDING;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Certificate c)) return false;
        return id != null && id.equals(c.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
