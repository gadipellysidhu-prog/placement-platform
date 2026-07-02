package com.college.placement.shared.academic.controller;

import com.college.placement.shared.academic.dto.AcademicYearCreateRequest;
import com.college.placement.shared.academic.dto.AcademicYearResponse;
import com.college.placement.shared.academic.dto.AcademicYearUpdateRequest;
import com.college.placement.shared.academic.service.AcademicYearService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/academic-years")
@Tag(name = "Academic Years", description = "Academic year / placement season administration")
public class AcademicYearController {

    private final AcademicYearService service;

    public AcademicYearController(AcademicYearService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create an academic year")
    public ResponseEntity<AcademicYearResponse> create(@Valid @RequestBody AcademicYearCreateRequest req) {
        var year = service.create(req.label(), req.startDate(), req.endDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(AcademicYearResponse.from(year));
    }

    @GetMapping
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "List academic years (paginated)")
    public ResponseEntity<Page<AcademicYearResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(service.getAll(pageable).map(AcademicYearResponse::from));
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Get the currently active academic year")
    public ResponseEntity<AcademicYearResponse> active() {
        return ResponseEntity.ok(AcademicYearResponse.from(service.requireActive()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Get academic year by ID")
    public ResponseEntity<AcademicYearResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(AcademicYearResponse.from(service.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an academic year's dates")
    public ResponseEntity<AcademicYearResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody AcademicYearUpdateRequest req) {
        var year = service.update(id, req.startDate(), req.endDate());
        return ResponseEntity.ok(AcademicYearResponse.from(year));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate an academic year (deactivates any other active year)")
    public ResponseEntity<AcademicYearResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(AcademicYearResponse.from(service.activate(id)));
    }
}
