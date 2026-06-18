package com.college.placement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: verifies the Spring context loads with the test profile (H2 in-memory DB).
 * Testcontainers-based integration tests live in a separate class and require Docker.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty: failure to start the context fails the test.
    }
}
