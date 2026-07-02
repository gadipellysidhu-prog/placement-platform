package com.college.placement.shared.settings.controller;

import com.college.placement.shared.settings.dto.SettingResponse;
import com.college.placement.shared.settings.dto.SettingUpsertRequest;
import com.college.placement.shared.settings.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/settings")
@Tag(name = "Settings", description = "Configuration-driven application settings (admin)")
@PreAuthorize("hasRole('ADMIN')")
public class SettingsController {

    private final SettingsService service;

    public SettingsController(SettingsService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List settings (optionally filtered by category)")
    public ResponseEntity<Page<SettingResponse>> list(@RequestParam(required = false) String category,
                                                       Pageable pageable) {
        return ResponseEntity.ok(service.list(category, pageable).map(SettingResponse::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a setting by ID")
    public ResponseEntity<SettingResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(SettingResponse.from(service.getById(id)));
    }

    @PutMapping
    @Operation(summary = "Create or update a setting (upsert by key + academic year)")
    public ResponseEntity<SettingResponse> upsert(@Valid @RequestBody SettingUpsertRequest req) {
        var saved = service.upsert(req.settingKey(), req.settingValue(), req.valueType(),
                req.category(), req.description(), req.academicYearId());
        return ResponseEntity.ok(SettingResponse.from(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a setting")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
