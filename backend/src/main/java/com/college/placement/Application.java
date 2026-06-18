package com.college.placement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Placement Intelligence &amp; Skill Verification Platform.
 *
 * <p>The application is a <strong>modular monolith</strong> (SADD §1.2, §3): a single
 * deployable Spring Boot unit whose code is organised into independent bounded-context
 * modules under {@code com.college.placement.modules} and cross-cutting infrastructure
 * under {@code com.college.placement.shared}.
 *
 * <p>{@link EnableScheduling} is enabled here because the transactional outbox poller and
 * other background jobs (added in later increments) run as scheduled tasks inside this JVM
 * rather than as separate services (SADD §5.3, §13.1).
 */
@SpringBootApplication
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
