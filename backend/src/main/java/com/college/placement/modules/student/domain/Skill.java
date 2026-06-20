package com.college.placement.modules.student.domain;

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
