package com.college.placement.modules.jobintelligence.ai;

/**
 * LLM provider port. Implementations call a concrete model API and return the raw
 * completion text (expected to be JSON — validation happens downstream, never here).
 * The active provider is chosen purely by configuration; no code changes needed to
 * switch models or vendors.
 */
public interface AIProvider {

    /** Stable identifier recorded on runs and inference history (e.g. "openai-compatible"). */
    String id();

    /** The model name this provider is configured to call. */
    String model();

    /**
     * Execute the prompt and return the raw completion text.
     *
     * @throws AIProviderException with {@code transient=true} for retryable failures
     *         (timeouts, 429, 5xx) and {@code transient=false} for permanent ones.
     */
    CompletionResult complete(String prompt);

    /** Cheap availability probe for health/metrics; must never throw. */
    boolean healthy();

    /** Raw completion plus token accounting for the governance ledger. */
    record CompletionResult(String text, int inputTokens, int outputTokens, long latencyMs) {}

    class AIProviderException extends RuntimeException {
        private final boolean transientFailure;

        public AIProviderException(String message, boolean transientFailure) {
            super(message);
            this.transientFailure = transientFailure;
        }

        public AIProviderException(String message, boolean transientFailure, Throwable cause) {
            super(message, cause);
            this.transientFailure = transientFailure;
        }

        public boolean isTransient() {
            return transientFailure;
        }
    }
}
