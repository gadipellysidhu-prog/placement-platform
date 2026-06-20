package com.college.placement.shared.ratelimit;

import org.springframework.context.annotation.Configuration;

/**
 * Rate limiting placeholder.
 *
 * When business API endpoints are introduced, add:
 *   <dependency>
 *       <groupId>com.bucket4j</groupId>
 *       <artifactId>bucket4j-spring-boot-starter</artifactId>
 *       <version>8.x</version>
 *   </dependency>
 *
 * Then configure per-endpoint policies in application.yml under:
 *   bucket4j:
 *     filters:
 *       - cache-name: rate-limit
 *         url: /api/.*
 *         rate-limits:
 *           - bandwidths:
 *               - capacity: 20
 *                 time: 1
 *                 unit: minutes
 */
@Configuration
public class RateLimitConfig {
    // Policies are defined per-endpoint when REST controllers are introduced.
}
