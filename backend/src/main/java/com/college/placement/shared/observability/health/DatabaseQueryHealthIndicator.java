package com.college.placement.shared.observability.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("databaseQuery")
public class DatabaseQueryHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseQueryHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        long start = System.currentTimeMillis();
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            long latencyMs = System.currentTimeMillis() - start;
            return Health.up()
                    .withDetail("queryLatencyMs", latencyMs)
                    .withDetail("query", "SELECT 1")
                    .build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }
}
