package com.college.placement.shared.observability.health;

import com.college.placement.shared.eventbus.DomainEvent;
import com.college.placement.shared.eventbus.EventPublisher;
import com.college.placement.shared.eventbus.routing.EventRoutingValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component("eventBus")
public class EventBusHealthIndicator implements HealthIndicator {

    private final EventPublisher eventPublisher;
    private final ApplicationContext applicationContext;

    public EventBusHealthIndicator(EventPublisher eventPublisher,
                                   ApplicationContext applicationContext) {
        this.eventPublisher = eventPublisher;
        this.applicationContext = applicationContext;
    }

    @Override
    public Health health() {
        boolean publisherAvailable = eventPublisher != null;
        int listenerCount = countDomainEventListeners();
        boolean routingInitialized = listenerCount > 0;

        if (!publisherAvailable) {
            return Health.down()
                    .withDetail("reason", "EventPublisher bean not available")
                    .build();
        }
        if (!routingInitialized) {
            return Health.down()
                    .withDetail("reason", "No domain event listeners registered")
                    .build();
        }

        return Health.up()
                .withDetail("type", "SpringApplicationEventPublisher")
                .withDetail("transport", "in-process")
                .withDetail("publisherAvailable", true)
                .withDetail("listenerCount", listenerCount)
                .withDetail("routingInitialized", true)
                .build();
    }

    private int countDomainEventListeners() {
        AtomicInteger count = new AtomicInteger(0);
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            try {
                Object bean = applicationContext.getBean(beanName);
                for (Method method : bean.getClass().getMethods()) {
                    if (method.isAnnotationPresent(TransactionalEventListener.class)
                            && method.getParameterCount() == 1
                            && DomainEvent.class.isAssignableFrom(method.getParameterTypes()[0])) {
                        count.incrementAndGet();
                    }
                }
            } catch (Exception ignored) {
                // Skip beans that can't be retrieved
            }
        }
        return count.get();
    }
}
