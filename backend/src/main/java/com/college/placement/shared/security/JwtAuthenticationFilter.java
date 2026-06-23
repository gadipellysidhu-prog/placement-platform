package com.college.placement.shared.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION  = "Authorization";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final SecurityAuditLogger auditLogger;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UserDetailsService userDetailsService,
                                   SecurityAuditLogger auditLogger) {
        this.jwtService         = jwtService;
        this.userDetailsService = userDetailsService;
        this.auditLogger        = auditLogger;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String header = request.getHeader(AUTHORIZATION);
            if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
                String token = header.substring(BEARER_PREFIX.length());
                var claims = jwtService.validateAndParse(token);
                if (claims.isPresent()) {
                    setAuthentication(request, claims.get());
                } else {
                    auditLogger.jwtValidationFailure(request.getRequestURI(), extractIp(request), "token rejected");
                }
            }
        } catch (Exception ex) {
            auditLogger.jwtValidationFailure(request.getRequestURI(), extractIp(request), ex.getMessage());
            log.debug("JWT filter error on {}: {}", request.getRequestURI(), ex.getMessage());
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }

    private void setAuthentication(HttpServletRequest request, Claims claims) {
        try {
            String subject = claims.getSubject();
            if (!StringUtils.hasText(subject)) {
                return;
            }
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                return;
            }
            UserDetails userDetails = userDetailsService.loadUserByUsername(subject);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception ex) {
            log.debug("Could not set authentication: {}", ex.getMessage());
        }
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
