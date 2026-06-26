package com.college.placement.modules.student.controller;

import com.college.placement.modules.student.dto.SkillCreateRequest;
import com.college.placement.modules.student.dto.SkillResponse;
import com.college.placement.modules.student.dto.SkillUpdateRequest;
import com.college.placement.modules.student.service.SkillService;
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
@RequestMapping("/api/skills")
@Tag(name = "Skills", description = "Skill catalogue management")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Create a new skill")
    public ResponseEntity<SkillResponse> create(@Valid @RequestBody SkillCreateRequest req) {
        var skill = skillService.createSkill(req.name(), req.category());
        return ResponseEntity.status(HttpStatus.CREATED).body(SkillResponse.from(skill));
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "List skills, optionally filtered by category or verified flag")
    public ResponseEntity<List<SkillResponse>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean verified) {
        List<SkillResponse> result;
        if (category != null) {
            result = skillService.getByCategory(category).stream().map(SkillResponse::from).toList();
        } else if (Boolean.TRUE.equals(verified)) {
            result = skillService.getVerified().stream().map(SkillResponse::from).toList();
        } else {
            result = skillService.getAll().stream().map(SkillResponse::from).toList();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get skill by ID")
    public ResponseEntity<SkillResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(SkillResponse.from(skillService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Update a skill")
    public ResponseEntity<SkillResponse> update(@PathVariable UUID id,
                                                 @Valid @RequestBody SkillUpdateRequest req) {
        var skill = skillService.updateSkill(id, req.name(), req.category());
        return ResponseEntity.ok(SkillResponse.from(skill));
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @Operation(summary = "Verify a skill")
    public ResponseEntity<SkillResponse> verify(@PathVariable UUID id) {
        return ResponseEntity.ok(SkillResponse.from(skillService.verify(id)));
    }
}
