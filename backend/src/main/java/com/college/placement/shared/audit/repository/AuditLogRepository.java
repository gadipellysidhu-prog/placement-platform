package com.college.placement.shared.audit.repository;

import com.college.placement.shared.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

    Page<AuditLog> findByPerformedBy(String performedBy, Pageable pageable);

    /**
     * Admin audit-trail listing with optional filters (any may be null). Exact match on the
     * indexed categorical fields (entityType/entityId/action), a case-insensitive contains
     * match on the free-text actor, and an inclusive created-at range. Mirrors
     * {@code AppUserRepository.search}; the indexes on (entity_type, entity_id), performed_by
     * and created_at back the common filter and sort paths.
     *
     * <p>The nullable parameters are cast explicitly. PostgreSQL cannot infer a bind's type
     * from a bare {@code ? IS NULL} test, so an untyped null either resolves to {@code bytea}
     * ("function lower(bytea) does not exist") or is rejected outright ("could not determine
     * data type of parameter"). The casts are inert on H2, which is why the test suite —
     * which runs on H2 — never saw either failure.
     */
    @Query("SELECT a FROM AuditLog a WHERE (:entityType IS NULL OR a.entityType = :entityType) "
            + "AND (:entityId IS NULL OR a.entityId = :entityId) "
            + "AND (:action IS NULL OR a.action = :action) "
            + "AND (:performedBy IS NULL OR LOWER(a.performedBy) LIKE LOWER(CONCAT('%', CAST(:performedBy AS String), '%'))) "
            + "AND (CAST(:dateFrom AS Instant) IS NULL OR a.createdAt >= :dateFrom) "
            + "AND (CAST(:dateTo AS Instant) IS NULL OR a.createdAt <= :dateTo)")
    Page<AuditLog> search(@Param("entityType") String entityType,
                          @Param("entityId") String entityId,
                          @Param("action") String action,
                          @Param("performedBy") String performedBy,
                          @Param("dateFrom") Instant dateFrom,
                          @Param("dateTo") Instant dateTo,
                          Pageable pageable);
}
