package com.college.placement.modules.certificate.controller;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.certificate.dto.CertificateCreateRequest;
import com.college.placement.modules.certificate.dto.CertificateResponse;
import com.college.placement.modules.certificate.service.CertificateService;
import com.college.placement.modules.student.domain.Student;
import com.college.placement.modules.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/certificates")
@Tag(name = "Certificates", description = "Certificate submission and verification")
public class CertificateController {

    private final CertificateService certificateService;
    private final StudentService studentService;
    private final AppUserRepository appUserRepository;

    public CertificateController(CertificateService certificateService,
                                  StudentService studentService,
                                  AppUserRepository appUserRepository) {
        this.certificateService = certificateService;
        this.studentService = studentService;
        this.appUserRepository = appUserRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Submit a certificate for verification")
    public ResponseEntity<CertificateResponse> submit(@Valid @RequestBody CertificateCreateRequest req,
                                                      Authentication auth) {
        AppUser user = appUserRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Student ownStudent = studentService.getByUser(user);
        if (!ownStudent.getId().equals(req.studentId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only submit certificates for yourself");
        }
        var cert = certificateService.submitCertificate(
                req.studentId(), req.name(), req.issuingOrganization(), req.skillId(), req.fileKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(CertificateResponse.from(cert));
    }

    @GetMapping
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "List all certificates (paginated)")
    public ResponseEntity<Page<CertificateResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(certificateService.getAll(pageable).map(CertificateResponse::from));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get own certificates (authenticated student)")
    public ResponseEntity<List<CertificateResponse>> my(Authentication auth) {
        AppUser user = appUserRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Student student = studentService.getByUser(user);
        return ResponseEntity.ok(
                certificateService.getByStudent(student.getId()).stream()
                        .map(CertificateResponse::from).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Get certificate by ID")
    public ResponseEntity<CertificateResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(CertificateResponse.from(certificateService.getById(id)));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Get all certificates for a student")
    public ResponseEntity<List<CertificateResponse>> byStudent(@PathVariable UUID studentId) {
        return ResponseEntity.ok(
                certificateService.getByStudent(studentId).stream()
                        .map(CertificateResponse::from).toList());
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Verify a certificate")
    public ResponseEntity<CertificateResponse> verify(@PathVariable UUID id) {
        return ResponseEntity.ok(CertificateResponse.from(certificateService.verify(id)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Reject a certificate")
    public ResponseEntity<CertificateResponse> reject(@PathVariable UUID id) {
        return ResponseEntity.ok(CertificateResponse.from(certificateService.reject(id)));
    }
}
