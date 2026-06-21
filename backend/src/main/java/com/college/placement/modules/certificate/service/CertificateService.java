package com.college.placement.modules.certificate.service;

import com.college.placement.modules.certificate.domain.Certificate;
import com.college.placement.modules.certificate.domain.CertificateVerificationStatus;
import com.college.placement.modules.certificate.event.CertificateRejectedEvent;
import com.college.placement.modules.certificate.repository.CertificateRepository;
import com.college.placement.modules.student.domain.Skill;
import com.college.placement.modules.student.domain.Student;
import com.college.placement.modules.student.service.SkillService;
import com.college.placement.modules.student.service.StudentService;
import com.college.placement.shared.eventbus.EventPublisher;
import com.college.placement.shared.eventbus.events.CertificateCreatedEvent;
import com.college.placement.shared.eventbus.events.CertificateVerifiedEvent;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final StudentService studentService;
    private final SkillService skillService;
    private final EventPublisher eventPublisher;

    public CertificateService(CertificateRepository certificateRepository,
                               StudentService studentService,
                               SkillService skillService,
                               EventPublisher eventPublisher) {
        this.certificateRepository = certificateRepository;
        this.studentService = studentService;
        this.skillService = skillService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Certificate submitCertificate(UUID studentId, String name, String issuingOrganization,
                                          UUID skillId, String fileKey) {
        Student student = studentService.getById(studentId);
        Certificate certificate = new Certificate();
        certificate.setStudent(student);
        certificate.setName(name);
        certificate.setIssuingOrganization(issuingOrganization);
        certificate.setFileKey(fileKey);
        if (skillId != null) {
            Skill skill = skillService.getById(skillId);
            certificate.setSkill(skill);
        }
        Certificate saved = certificateRepository.save(certificate);
        eventPublisher.publish(CertificateCreatedEvent.of(saved.getId(), studentId, name));
        return saved;
    }

    @Transactional
    public Certificate verify(UUID certificateId) {
        Certificate certificate = getById(certificateId);
        requirePending(certificate);
        certificate.setVerificationStatus(CertificateVerificationStatus.VERIFIED);
        Certificate saved = certificateRepository.save(certificate);
        eventPublisher.publish(CertificateVerifiedEvent.of(
                certificateId, certificate.getStudent().getId(), certificate.getName()));
        return saved;
    }

    @Transactional
    public Certificate reject(UUID certificateId) {
        Certificate certificate = getById(certificateId);
        requirePending(certificate);
        certificate.setVerificationStatus(CertificateVerificationStatus.REJECTED);
        Certificate saved = certificateRepository.save(certificate);
        eventPublisher.publish(CertificateRejectedEvent.of(
                certificateId, certificate.getStudent().getId(), certificate.getName()));
        return saved;
    }

    @Transactional(readOnly = true)
    public Certificate getById(UUID id) {
        return certificateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found"));
    }

    @Transactional(readOnly = true)
    public List<Certificate> getByStudent(UUID studentId) {
        Student student = studentService.getById(studentId);
        return certificateRepository.findByStudent(student);
    }

    @Transactional(readOnly = true)
    public List<Certificate> getByStudentAndStatus(UUID studentId, CertificateVerificationStatus status) {
        Student student = studentService.getById(studentId);
        return certificateRepository.findByStudentAndVerificationStatus(student, status);
    }

    private void requirePending(Certificate certificate) {
        if (certificate.getVerificationStatus() != CertificateVerificationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Certificate is not in PENDING status; current: " + certificate.getVerificationStatus());
        }
    }
}
