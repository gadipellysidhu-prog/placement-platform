package com.college.placement.modules.certificate.repository;

import com.college.placement.modules.certificate.domain.Certificate;
import com.college.placement.modules.certificate.domain.CertificateVerificationStatus;
import com.college.placement.modules.student.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    List<Certificate> findByStudent(Student student);

    List<Certificate> findByStudentAndVerificationStatus(Student student, CertificateVerificationStatus status);
}
