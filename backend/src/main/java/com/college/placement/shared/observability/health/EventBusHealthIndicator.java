package com.college.placement.shared.observability.health;

import com.college.placement.shared.eventbus.EventPublisher;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("eventBus")
public class EventBusHealthIndicator implements HealthIndicator {

    private final EventPublisher eventPublisher;

    public EventBusHealthIndicator(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("type", "SpringApplicationEventPublisher")
                .withDetail("transport", "in-process")
                .withDetail("available", true)
                .build();
    }
}
