package com.college.placement.modules.company.controller;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.company.domain.Recruiter;
import com.college.placement.modules.company.dto.RecruiterRegisterRequest;
import com.college.placement.modules.company.dto.RecruiterResponse;
import com.college.placement.modules.company.service.RecruiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/recruiters")
@Tag(name = "Recruiters", description = "Recruiter profile management")
public class RecruiterController {

    private final RecruiterService recruiterService;
    private final AppUserRepository appUserRepository;

    public RecruiterController(RecruiterService recruiterService, AppUserRepository appUserRepository) {
        this.recruiterService = recruiterService;
        this.appUserRepository = appUserRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Register a recruiter profile", responses = {
            @ApiResponse(responseCode = "201", description = "Recruiter registered"),
            @ApiResponse(responseCode = "404", description = "User or company not found"),
            @ApiResponse(responseCode = "409", description = "Recruiter profile already exists for this user")
    })
    public ResponseEntity<RecruiterResponse> register(@Valid @RequestBody RecruiterRegisterRequest req) {
        AppUser user = appUserRepository.findById(req.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Recruiter recruiter = recruiterService.registerRecruiter(user, req.companyId(), req.designation());
        return ResponseEntity.status(HttpStatus.CREATED).body(RecruiterResponse.from(recruiter));
    }
}
