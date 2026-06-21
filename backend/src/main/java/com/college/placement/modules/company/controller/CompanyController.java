package com.college.placement.modules.company.controller;

import com.college.placement.modules.company.dto.CompanyCreateRequest;
import com.college.placement.modules.company.dto.CompanyResponse;
import com.college.placement.modules.company.dto.CompanyUpdateRequest;
import com.college.placement.modules.company.service.CompanyService;
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
@RequestMapping("/api/companies")
@Tag(name = "Companies", description = "Company management")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Register a new company")
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CompanyCreateRequest req) {
        var company = companyService.createCompany(req.name(), req.website(), req.industry(), req.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(CompanyResponse.from(company));
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "List all companies (paginated)")
    public ResponseEntity<Page<CompanyResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(companyService.getAll(pageable).map(CompanyResponse::from));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get company by ID")
    public ResponseEntity<CompanyResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(CompanyResponse.from(companyService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Update company details")
    public ResponseEntity<CompanyResponse> update(@PathVariable UUID id,
                                                   @Valid @RequestBody CompanyUpdateRequest req) {
        var company = companyService.updateCompany(id, req.name(), req.website(), req.industry(),
                req.description(), req.logoUrl());
        return ResponseEntity.ok(CompanyResponse.from(company));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Activate a company")
    public ResponseEntity<CompanyResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(CompanyResponse.from(companyService.activate(id)));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Deactivate a company")
    public ResponseEntity<CompanyResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(CompanyResponse.from(companyService.deactivate(id)));
    }

    @PostMapping("/{id}/blacklist")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Blacklist a company (Admin only)")
    public ResponseEntity<CompanyResponse> blacklist(@PathVariable UUID id) {
        return ResponseEntity.ok(CompanyResponse.from(companyService.blacklist(id)));
    }
}
