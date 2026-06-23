package com.college.placement.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Writes structured SECURITY_EVENT log lines.
 * Fields are space-separated key=value pairs — intentionally no passwords or tokens.
 */
@Slf4j
@Component
public class SecurityAuditLogger {

    public void loginSuccess(String email, String ip) {
        log.info("SECURITY_EVENT event=LOGIN_SUCCESS email={} ip={}", sanitize(email), sanitize(ip));
    }

    public void loginFailure(String email, String ip, String reason) {
        log.warn("SECURITY_EVENT event=LOGIN_FAILURE email={} ip={} reason={}", sanitize(email), sanitize(ip), reason);
    }

    public void accountLocked(String email, String ip) {
        log.warn("SECURITY_EVENT event=ACCOUNT_LOCKED email={} ip={}", sanitize(email), sanitize(ip));
    }

    public void jwtValidationFailure(String uri, String ip, String reason) {
        log.warn("SECURITY_EVENT event=JWT_VALIDATION_FAILURE uri={} ip={} reason={}", sanitize(uri), sanitize(ip), reason);
    }

    public void accessDenied(String uri, String ip, String principal) {
        log.warn("SECURITY_EVENT event=ACCESS_DENIED uri={} ip={} principal={}", sanitize(uri), sanitize(ip), sanitize(principal));
    }

    public void rateLimitExceeded(String uri, String ip, String endpoint) {
        log.warn("SECURITY_EVENT event=RATE_LIMIT_EXCEEDED uri={} ip={} endpoint={}", sanitize(uri), sanitize(ip), endpoint);
    }

    private String sanitize(String value) {
        if (value == null) return "unknown";
        return value.replaceAll("[\r\n\t]", "_").trim();
    }
}
