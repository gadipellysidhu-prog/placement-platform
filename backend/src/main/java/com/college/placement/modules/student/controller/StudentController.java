package com.college.placement.modules.student.controller;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.student.domain.Student;
import com.college.placement.modules.student.dto.*;
import com.college.placement.modules.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/students")
@Tag(name = "Students", description = "Student profile management")
public class StudentController {

    private final StudentService studentService;
    private final AppUserRepository appUserRepository;

    public StudentController(StudentService studentService, AppUserRepository appUserRepository) {
        this.studentService = studentService;
        this.appUserRepository = appUserRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Create a student profile", responses = {
            @ApiResponse(responseCode = "201", description = "Student created"),
            @ApiResponse(responseCode = "409", description = "Profile already exists or roll number taken")
    })
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentCreateRequest req) {
        AppUser user = appUserRepository.findById(req.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Student student = studentService.createStudent(user, req.rollNumber(), req.branchId(), req.currentYear());
        return ResponseEntity.status(HttpStatus.CREATED).body(StudentResponse.from(student));
    }

    @GetMapping
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "List all students (paginated)")
    public ResponseEntity<Page<StudentResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(studentService.getAll(pageable).map(StudentResponse::from));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get own student profile (authenticated student)")
    public ResponseEntity<StudentResponse> me(Authentication auth) {
        AppUser user = appUserRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Student student = studentService.getByUser(user);
        return ResponseEntity.ok(StudentResponse.from(student));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Get student by ID")
    public ResponseEntity<StudentResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(StudentResponse.from(studentService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Update student profile (branch, CGPA, year)")
    public ResponseEntity<StudentResponse> update(@PathVariable UUID id,
                                                   @Valid @RequestBody StudentUpdateRequest req) {
        Student student = studentService.updateProfile(id, req.branchId(), req.cgpa(), req.currentYear());
        return ResponseEntity.ok(StudentResponse.from(student));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Update student placement status")
    public ResponseEntity<StudentResponse> updateStatus(@PathVariable UUID id,
                                                         @Valid @RequestBody StudentStatusUpdateRequest req) {
        Student student = studentService.updateStatus(id, req.status());
        return ResponseEntity.ok(StudentResponse.from(student));
    }

    @PutMapping("/{id}/eligibility")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Re-evaluate placement eligibility")
    public ResponseEntity<StudentResponse> evaluateEligibility(@PathVariable UUID id) {
        return ResponseEntity.ok(StudentResponse.from(studentService.evaluateEligibility(id)));
    }

    @PostMapping("/{id}/skills/{skillId}")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Assign a skill to a student")
    public ResponseEntity<StudentResponse> assignSkill(@PathVariable UUID id, @PathVariable UUID skillId) {
        return ResponseEntity.ok(StudentResponse.from(studentService.assignSkill(id, skillId)));
    }

    @DeleteMapping("/{id}/skills/{skillId}")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Remove a skill from a student")
    public ResponseEntity<StudentResponse> removeSkill(@PathVariable UUID id, @PathVariable UUID skillId) {
        return ResponseEntity.ok(StudentResponse.from(studentService.removeSkill(id, skillId)));
    }
}
