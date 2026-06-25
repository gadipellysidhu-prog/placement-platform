package com.college.placement.modules.placement.controller;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.placement.dto.OfferCreateRequest;
import com.college.placement.modules.placement.dto.OfferResponse;
import com.college.placement.modules.placement.service.JobApplicationService;
import com.college.placement.modules.placement.service.OfferService;
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
@RequestMapping("/api/offers")
@Tag(name = "Offers", description = "Placement offer management")
public class OfferController {

    private final OfferService offerService;
    private final JobApplicationService jobApplicationService;
    private final StudentService studentService;
    private final AppUserRepository appUserRepository;

    public OfferController(OfferService offerService,
                            JobApplicationService jobApplicationService,
                            StudentService studentService,
                            AppUserRepository appUserRepository) {
        this.offerService = offerService;
        this.jobApplicationService = jobApplicationService;
        this.studentService = studentService;
        this.appUserRepository = appUserRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Create a placement offer for an OFFERED application")
    public ResponseEntity<OfferResponse> create(@Valid @RequestBody OfferCreateRequest req) {
        var offer = offerService.createOffer(req.applicationId(), req.ctc(), req.joiningDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(OfferResponse.from(offer));
    }

    @GetMapping
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "List all offers (paginated)")
    public ResponseEntity<Page<OfferResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(offerService.getAll(pageable).map(OfferResponse::from));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get own offers (authenticated student)")
    public ResponseEntity<List<OfferResponse>> my(Authentication auth) {
        AppUser user = appUserRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Student student = studentService.getByUser(user);
        List<OfferResponse> offers = jobApplicationService.getByStudent(student.getId()).stream()
                .flatMap(app -> {
                    try {
                        return java.util.stream.Stream.of(offerService.getByApplication(app.getId()));
                    } catch (org.springframework.web.server.ResponseStatusException ex) {
                        return java.util.stream.Stream.empty();
                    }
                })
                .map(OfferResponse::from)
                .toList();
        return ResponseEntity.ok(offers);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Get offer by ID")
    public ResponseEntity<OfferResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(OfferResponse.from(offerService.getById(id)));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Accept a PENDING offer")
    public ResponseEntity<OfferResponse> accept(@PathVariable UUID id, Authentication auth) {
        AppUser user = appUserRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Student ownStudent = studentService.getByUser(user);
        return ResponseEntity.ok(OfferResponse.from(offerService.acceptOffer(id, ownStudent.getId())));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Reject a PENDING offer")
    public ResponseEntity<OfferResponse> reject(@PathVariable UUID id, Authentication auth) {
        AppUser user = appUserRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Student ownStudent = studentService.getByUser(user);
        return ResponseEntity.ok(OfferResponse.from(offerService.rejectOffer(id, ownStudent.getId())));
    }

    @PostMapping("/{id}/expire")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Mark a PENDING offer as expired")
    public ResponseEntity<OfferResponse> expire(@PathVariable UUID id) {
        return ResponseEntity.ok(OfferResponse.from(offerService.expireOffer(id)));
    }
}
