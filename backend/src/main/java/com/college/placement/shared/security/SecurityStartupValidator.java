package com.college.placement.shared.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that all required production secrets are present at startup.
 * Prevents the application from starting in prod with missing credentials.
 * Active only when the "prod" profile is loaded.
 */
@Slf4j
@Component
@Profile("prod")
public class SecurityStartupValidator {

    @Value("${jwt.private-key-pem:}") private String jwtPrivateKey;
    @Value("${jwt.public-key-pem:}")  private String jwtPublicKey;
    @Value("${spring.datasource.url:}") private String dbUrl;
    @Value("${spring.datasource.username:}") private String dbUsername;
    @Value("${spring.datasource.password:}") private String dbPassword;
    @Value("${cors.allowed-origins:}") private String corsOrigins;

    @PostConstruct
    void validate() {
        List<String> missing = new ArrayList<>();

        if (!StringUtils.hasText(jwtPrivateKey))  missing.add("JWT_PRIVATE_KEY_PEM (jwt.private-key-pem)");
        if (!StringUtils.hasText(jwtPublicKey))   missing.add("JWT_PUBLIC_KEY_PEM (jwt.public-key-pem)");
        if (!StringUtils.hasText(dbUrl))          missing.add("DATABASE_URL (spring.datasource.url)");
        if (!StringUtils.hasText(dbUsername))     missing.add("DATABASE_USERNAME (spring.datasource.username)");
        if (!StringUtils.hasText(dbPassword))     missing.add("DATABASE_PASSWORD (spring.datasource.password)");
        if (!StringUtils.hasText(corsOrigins))    missing.add("CORS_ALLOWED_ORIGINS (cors.allowed-origins)");

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                "Production startup aborted — required secrets are missing: " + missing
            );
        }

        log.info("SECURITY_EVENT event=STARTUP_VALIDATION_PASSED profile=prod secrets_verified=all");
    }
}
