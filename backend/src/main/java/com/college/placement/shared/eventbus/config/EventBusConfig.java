package com.college.placement.shared.eventbus.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ErrorHandler;

import java.util.concurrent.Executor;

/**
 * Event bus infrastructure configuration.
 *
 * <p>{@code @EnableAsync} activates Spring's async method execution.
 * The {@code eventBusExecutor} bean is available for any handler that
 * opts into asynchronous execution via {@code @Async("eventBusExecutor")}.
 * Handlers without {@code @Async} continue to run synchronously in the
 * AFTER_COMMIT transaction synchronization callback — which is the default
 * and preferred behaviour for Phase 5.
 *
 * <p>The {@code applicationEventMulticaster} is configured with an error handler
 * that logs and continues, preventing one listener failure from crashing others.
 */
@Slf4j
@Configuration
@EnableAsync
public class EventBusConfig {

    @Bean(name = "eventBusExecutor")
    public Executor eventBusExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("event-bus-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Bean(name = "applicationEventMulticaster")
    public SimpleApplicationEventMulticaster applicationEventMulticaster() {
        SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
        multicaster.setErrorHandler(ex ->
                log.error("EVENT_FAILED handler threw uncaught exception — other listeners continue: {}",
                        ex.getMessage(), ex));
        return multicaster;
    }
}
