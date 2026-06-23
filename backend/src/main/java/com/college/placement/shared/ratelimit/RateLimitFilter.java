package com.college.placement.shared.ratelimit;

import com.college.placement.shared.security.SecurityAuditLogger;
import com.college.placement.shared.security.SecurityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_EP    = "login";
    private static final String REGISTER_EP = "register";
    private static final String REFRESH_EP  = "refresh";
    private static final String GENERAL_EP  = "general";

    private final SecurityProperties props;
    private final SecurityAuditLogger auditLogger;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(SecurityProperties props,
                           SecurityAuditLogger auditLogger,
                           ObjectMapper objectMapper) {
        this.props        = props;
        this.auditLogger  = auditLogger;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        if (!props.getRateLimit().isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        String ip       = extractIp(request);
        String endpoint = classifyEndpoint(request.getRequestURI());
        String key      = ip + ":" + endpoint;

        Bucket bucket = buckets.computeIfAbsent(key, k -> buildBucket(endpoint));

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            auditLogger.rateLimitExceeded(request.getRequestURI(), ip, endpoint);
            writeTooManyRequests(response, request.getRequestURI());
        }
    }

    /** Reset all in-memory buckets — used in integration tests between test cases. */
    public void clearAll() {
        buckets.clear();
    }

    private Bucket buildBucket(String endpoint) {
        SecurityProperties.RateLimitProperties rl = props.getRateLimit();
        int capacity = switch (endpoint) {
            case LOGIN_EP    -> rl.getLoginCapacity();
            case REGISTER_EP -> rl.getRegisterCapacity();
            case REFRESH_EP  -> rl.getRefreshCapacity();
            default          -> rl.getGeneralCapacity();
        };
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String classifyEndpoint(String uri) {
        if (uri == null) return GENERAL_EP;
        if (uri.endsWith("/auth/login"))    return LOGIN_EP;
        if (uri.endsWith("/auth/register")) return REGISTER_EP;
        if (uri.endsWith("/auth/refresh"))  return REFRESH_EP;
        return GENERAL_EP;
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response, String uri) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
        problem.setType(URI.create("urn:placement:rate-limit-exceeded"));
        problem.setTitle("Too Many Requests");
        problem.setDetail("Request rate limit exceeded. Please slow down and try again later.");
        problem.setInstance(URI.create(uri));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Retry-After", "60");
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
