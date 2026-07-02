package com.college.placement.shared.audit;

import org.slf4j.MDC;

/**
 * Read-only accessor for per-request audit metadata captured into the SLF4J
 * {@link MDC} by {@code RequestCorrelationFilter}. Returns {@code null} when
 * invoked outside an HTTP request (e.g. scheduled jobs), in which case the
 * audited action is attributed to the background context.
 */
public final class AuditContext {

    private AuditContext() {
    }

    public static String correlationId() {
        return MDC.get("requestId");
    }

    public static String ipAddress() {
        return MDC.get("clientIp");
    }

    public static String userAgent() {
        return MDC.get("userAgent");
    }
}
