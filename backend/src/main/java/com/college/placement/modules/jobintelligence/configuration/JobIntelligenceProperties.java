package com.college.placement.modules.jobintelligence.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Static configuration for the Job Intelligence pipeline (environment/yaml level).
 * Runtime kill-switches live in SettingsService (see JobIntelligenceFlags) so an
 * administrator can disable stages without a restart.
 *
 * <p>Secrets (API keys) are only ever injected via environment variables.
 */
@ConfigurationProperties(prefix = "job.intelligence")
public record JobIntelligenceProperties(
        /* Master enable for the module's beans/scheduling. */
        boolean enabled,
        Crawler crawler,
        Ai ai,
        Cache cache,
        Sweeper sweeper
) {
    public JobIntelligenceProperties {
        if (crawler == null) crawler = new Crawler(null, null, 0, 0, false);
        if (ai == null) ai = new Ai(null, null, null, null, 0, 0, null, 0);
        if (cache == null) cache = new Cache(null);
        if (sweeper == null) sweeper = new Sweeper(null);
    }

    public record Crawler(
            Duration connectTimeout,
            Duration readTimeout,
            int maxBodyBytes,
            int maxRedirects,
            /* SSRF guard escape hatch for local fixtures/tests ONLY. Never enable in prod. */
            boolean allowPrivateNetworks
    ) {
        public Crawler {
            if (connectTimeout == null) connectTimeout = Duration.ofSeconds(10);
            if (readTimeout == null) readTimeout = Duration.ofSeconds(20);
            if (maxBodyBytes <= 0) maxBodyBytes = 2 * 1024 * 1024; // 2 MB
            if (maxRedirects <= 0) maxRedirects = 3;
        }
    }

    public record Ai(
            /* "openai-compatible" (Groq/Ollama/OpenAI-style /chat/completions) or "stub". */
            String provider,
            String baseUrl,
            String apiKey,
            String model,
            double temperature,
            int maxTokens,
            Duration requestTimeout,
            int maxTransientRetries
    ) {
        public Ai {
            if (provider == null || provider.isBlank()) provider = "stub";
            if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://api.groq.com/openai/v1";
            if (model == null || model.isBlank()) model = "llama-3.3-70b-versatile";
            if (maxTokens <= 0) maxTokens = 2048;
            if (requestTimeout == null) requestTimeout = Duration.ofSeconds(60);
            if (maxTransientRetries <= 0) maxTransientRetries = 2;
        }
    }

    public record Cache(Duration ttl) {
        public Cache {
            if (ttl == null) ttl = Duration.ofDays(7);
        }
    }

    public record Sweeper(Duration stuckAfter) {
        public Sweeper {
            if (stuckAfter == null) stuckAfter = Duration.ofMinutes(10);
        }
    }
}
