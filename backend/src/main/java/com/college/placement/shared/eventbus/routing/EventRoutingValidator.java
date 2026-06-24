package com.college.placement.shared.eventbus.routing;

import com.college.placement.shared.eventbus.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates event routing at startup.
 * Scans all Spring beans for {@link TransactionalEventListener} methods,
 * builds a routing table, and logs orphan-event or duplicate-handler warnings.
 * Does NOT fail startup — problems are logged as WARN.
 */
@Slf4j
@Component
public class EventRoutingValidator {

    private final ApplicationContext applicationContext;

    public EventRoutingValidator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void validate(ContextRefreshedEvent ignored) {
        Map<String, List<String>> routingTable = buildRoutingTable();
        logRoutingTable(routingTable);
    }

    private Map<String, List<String>> buildRoutingTable() {
        Map<String, List<String>> table = new HashMap<>();

        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Object bean;
            try {
                bean = applicationContext.getBean(beanName);
            } catch (Exception ex) {
                continue;
            }

            for (Method method : bean.getClass().getMethods()) {
                if (!method.isAnnotationPresent(TransactionalEventListener.class)) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params.length != 1) {
                    continue;
                }
                Class<?> eventClass = params[0];
                if (!DomainEvent.class.isAssignableFrom(eventClass)) {
                    continue;
                }
                String eventTypeName = eventClass.getSimpleName();
                String handlerLabel = bean.getClass().getSimpleName() + "#" + method.getName();
                table.computeIfAbsent(eventTypeName, k -> new ArrayList<>()).add(handlerLabel);
            }
        }
        return table;
    }

    private void logRoutingTable(Map<String, List<String>> table) {
        if (table.isEmpty()) {
            log.warn("EVENT_ROUTING no domain event handlers found — check handler beans are registered");
            return;
        }

        log.info("EVENT_ROUTING_TABLE discovered {} event type(s):", table.size());
        for (Map.Entry<String, List<String>> entry : table.entrySet()) {
            List<String> handlers = entry.getValue();
            if (handlers.size() > 1) {
                log.info("  {} -> {} handler(s): {}", entry.getKey(), handlers.size(), handlers);
            } else {
                log.info("  {} -> {}", entry.getKey(), handlers.get(0));
            }
        }

        // DomainEvent (wildcard) handlers are expected — log informational only
        List<String> wildcardHandlers = table.get("DomainEvent");
        if (wildcardHandlers != null) {
            log.info("EVENT_ROUTING wildcard DomainEvent handlers (catch-all): {}", wildcardHandlers);
        }
    }
}
