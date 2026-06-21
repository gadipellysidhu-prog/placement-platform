package com.college.placement.shared.eventbus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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
 */
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
}
