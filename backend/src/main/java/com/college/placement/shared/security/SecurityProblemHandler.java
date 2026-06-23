package com.college.placement.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
public class SecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final SecurityAuditLogger auditLogger;

    public SecurityProblemHandler(ObjectMapper objectMapper, SecurityAuditLogger auditLogger) {
        this.objectMapper = objectMapper;
        this.auditLogger  = auditLogger;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        auditLogger.jwtValidationFailure(request.getRequestURI(), extractIp(request), authException.getMessage());
        write(response, HttpStatus.UNAUTHORIZED, "urn:placement:unauthorized", "Unauthorized");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        String principal = "anonymous";
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            principal = auth.getName();
        }
        auditLogger.accessDenied(request.getRequestURI(), extractIp(request), principal);
        write(response, HttpStatus.FORBIDDEN, "urn:placement:forbidden", "Forbidden");
    }

    private void write(HttpServletResponse response, HttpStatus status, String type, String title) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create(type));
        problem.setTitle(title);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
