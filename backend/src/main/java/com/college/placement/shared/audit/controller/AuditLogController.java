package com.college.placement.shared.audit.controller;

import com.college.placement.shared.audit.dto.AuditLogResponse;
import com.college.placement.shared.audit.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/admin/audit-logs")
@Tag(name = "Audit Logs", description = "Administrative audit trail (read-only, admin)")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "List/filter audit log entries (paginated). "
            + "Optional filters: entityType, entityId, action, performedBy, dateFrom, dateTo. "
            + "Sort with the standard 'sort' page parameter, e.g. sort=createdAt,desc.")
    public ResponseEntity<Page<AuditLogResponse>> list(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String performedBy,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            Pageable pageable) {
        return ResponseEntity.ok(
                auditService.search(entityType, entityId, action, performedBy, dateFrom, dateTo, pageable)
                        .map(AuditLogResponse::from));
    }
}
