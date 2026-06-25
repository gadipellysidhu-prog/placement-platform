package com.college.placement.modules.placement.controller;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.placement.dto.JobApplicationCreateRequest;
import com.college.placement.modules.placement.dto.JobApplicationResponse;
import com.college.placement.modules.placement.dto.JobApplicationStatusUpdateRequest;
import com.college.placement.modules.placement.service.JobApplicationService;
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
@RequestMapping("/api/applications")
@Tag(name = "Job Applications", description = "Student job application management")
public class PlacementController {

    private final JobApplicationService jobApplicationService;
    private final StudentService studentService;
    private final AppUserRepository appUserRepository;

    public PlacementController(JobApplicationService jobApplicationService,
                                StudentService studentService,
                                AppUserRepository appUserRepository) {
        this.jobApplicationService = jobApplicationService;
        this.studentService = studentService;
        this.appUserRepository = appUserRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Apply to a job posting")
    public ResponseEntity<JobApplicationResponse> apply(@Valid @RequestBody JobApplicationCreateRequest req,
                                                        Authentication auth) {
        AppUser user = appUserRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Student ownStudent = studentService.getByUser(user);
        if (!ownStudent.getId().equals(req.studentId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only apply for yourself");
        }
        var application = jobApplicationService.apply(req.studentId(), req.jobPostingId());
        return ResponseEntity.status(HttpStatus.CREATED).body(JobApplicationResponse.from(application));
    }

    @GetMapping
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "List all applications (paginated)")
    public ResponseEntity<Page<JobApplicationResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(jobApplicationService.getAll(pageable).map(JobApplicationResponse::from));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get own applications (authenticated student)")
    public ResponseEntity<List<JobApplicationResponse>> my(Authentication auth) {
        AppUser user = appUserRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Student student = studentService.getByUser(user);
        return ResponseEntity.ok(
                jobApplicationService.getByStudent(student.getId()).stream()
                        .map(JobApplicationResponse::from).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Get application by ID")
    public ResponseEntity<JobApplicationResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(JobApplicationResponse.from(jobApplicationService.getById(id)));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Get all applications for a student")
    public ResponseEntity<List<JobApplicationResponse>> byStudent(@PathVariable UUID studentId) {
        return ResponseEntity.ok(
                jobApplicationService.getByStudent(studentId).stream()
                        .map(JobApplicationResponse::from).toList());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Update application status (SHORTLISTED, INTERVIEWED, OFFERED, REJECTED)")
    public ResponseEntity<JobApplicationResponse> updateStatus(@PathVariable UUID id,
                                                                @Valid @RequestBody JobApplicationStatusUpdateRequest req) {
        return ResponseEntity.ok(JobApplicationResponse.from(jobApplicationService.updateStatus(id, req.status())));
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Withdraw an application")
    public ResponseEntity<JobApplicationResponse> withdraw(@PathVariable UUID id, Authentication auth) {
        AppUser user = appUserRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Student ownStudent = studentService.getByUser(user);
        return ResponseEntity.ok(JobApplicationResponse.from(jobApplicationService.withdraw(id, ownStudent.getId())));
    }
}
