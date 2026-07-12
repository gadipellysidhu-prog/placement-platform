package com.college.placement.modules.jobintelligence.configuration;

import com.college.placement.modules.jobintelligence.ai.AIProvider;
import com.college.placement.modules.jobintelligence.ai.OpenAICompatibleProvider;
import com.college.placement.modules.jobintelligence.ai.StubAIProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Locale;
import java.util.concurrent.Executor;

/**
 * Job Intelligence module wiring: the dedicated pipeline executor and the
 * configuration-selected AI provider. Switching model vendors is a property
 * change ({@code job.intelligence.ai.provider}) — never a code change.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(JobIntelligenceProperties.class)
public class JobIntelligenceConfig {

    /**
     * Dedicated pool for pipeline work. Sized small on purpose: extraction is
     * IO-bound and bursty, and isolating it means LLM latency can never starve the
     * shared event-bus executor or HTTP threads.
     */
    @Bean(name = "jobIntelligenceExecutor")
    public Executor jobIntelligenceExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("job-intel-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Bean
    public AIProvider aiProvider(JobIntelligenceProperties properties, ObjectMapper objectMapper) {
        String selected = properties.ai().provider().toLowerCase(Locale.ROOT);
        AIProvider provider = switch (selected) {
            case OpenAICompatibleProvider.ID, "groq", "ollama", "openai" ->
                    new OpenAICompatibleProvider(properties.ai(), objectMapper);
            case StubAIProvider.ID -> new StubAIProvider(objectMapper);
            default -> {
                log.warn("JOB_INTEL event=UNKNOWN_PROVIDER configured={} falling back to stub", selected);
                yield new StubAIProvider(objectMapper);
            }
        };
        log.info("JOB_INTEL event=PROVIDER_SELECTED provider={} model={}", provider.id(), provider.model());
        return provider;
    }
}
