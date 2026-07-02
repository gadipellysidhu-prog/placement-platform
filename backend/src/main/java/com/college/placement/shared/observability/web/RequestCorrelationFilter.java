package com.college.placement.shared.observability.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    private static final String HEADER_REQUEST_ID = "X-Request-ID";
    private static final String HEADER_TRACE_ID   = "X-Trace-ID";
    private static final String MDC_TRACE_ID       = "traceId";
    private static final String MDC_REQUEST_ID     = "requestId";
    private static final String MDC_CLIENT_IP      = "clientIp";
    private static final String MDC_USER_AGENT     = "userAgent";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String requestId = Optional.ofNullable(request.getHeader(HEADER_REQUEST_ID))
                .filter(v -> !v.isBlank())
                .orElse(UUID.randomUUID().toString());
        String traceId = UUID.randomUUID().toString();

        MDC.put(MDC_TRACE_ID, traceId);
        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put(MDC_CLIENT_IP, resolveClientIp(request));
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && !userAgent.isBlank()) {
            MDC.put(MDC_USER_AGENT, userAgent.length() > 512 ? userAgent.substring(0, 512) : userAgent);
        }

        response.setHeader(HEADER_TRACE_ID, traceId);
        response.setHeader(HEADER_REQUEST_ID, requestId);

        long start = System.currentTimeMillis();
        try {
            log.info("REQUEST_RECEIVED method={} uri={} requestId={}",
                    request.getMethod(), request.getRequestURI(), requestId);
            chain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            log.info("REQUEST_COMPLETED method={} uri={} status={} durationMs={}",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), durationMs);
            MDC.remove(MDC_TRACE_ID);
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_CLIENT_IP);
            MDC.remove(MDC_USER_AGENT);
        }
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // first hop is the originating client
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
