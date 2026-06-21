package com.college.placement.modules.certificate.repository;

import com.college.placement.modules.certificate.domain.Certificate;
import com.college.placement.modules.certificate.domain.CertificateVerificationStatus;
import com.college.placement.modules.student.domain.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    @EntityGraph(attributePaths = {"student", "student.user", "skill"})
    Optional<Certificate> findById(UUID id);

    @EntityGraph(attributePaths = {"student", "student.user", "skill"})
    Page<Certificate> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"student", "student.user", "skill"})
    List<Certificate> findByStudent(Student student);

    List<Certificate> findByStudentAndVerificationStatus(Student student, CertificateVerificationStatus status);
}
