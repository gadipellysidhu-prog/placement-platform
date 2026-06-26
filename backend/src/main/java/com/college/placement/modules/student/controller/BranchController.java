package com.college.placement.modules.student.controller;

import com.college.placement.modules.student.dto.BranchCreateRequest;
import com.college.placement.modules.student.dto.BranchResponse;
import com.college.placement.modules.student.dto.BranchUpdateRequest;
import com.college.placement.modules.student.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/branches")
@Tag(name = "Branches", description = "Academic branch management")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Create a new branch")
    public ResponseEntity<BranchResponse> create(@Valid @RequestBody BranchCreateRequest req) {
        var branch = branchService.createBranch(req.name(), req.code(), req.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(BranchResponse.from(branch));
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "List all active branches")
    public ResponseEntity<List<BranchResponse>> list() {
        return ResponseEntity.ok(branchService.getActiveBranches().stream()
                .map(BranchResponse::from).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get branch by ID")
    public ResponseEntity<BranchResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(BranchResponse.from(branchService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Update a branch")
    public ResponseEntity<BranchResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody BranchUpdateRequest req) {
        var branch = branchService.updateBranch(id, req.name(), req.code(), req.description());
        return ResponseEntity.ok(BranchResponse.from(branch));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Activate a branch")
    public ResponseEntity<BranchResponse> activate(@PathVariable UUID id) {
        branchService.activate(id);
        return ResponseEntity.ok(BranchResponse.from(branchService.getById(id)));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Deactivate a branch")
    public ResponseEntity<BranchResponse> deactivate(@PathVariable UUID id) {
        branchService.deactivate(id);
        return ResponseEntity.ok(BranchResponse.from(branchService.getById(id)));
    }
}
