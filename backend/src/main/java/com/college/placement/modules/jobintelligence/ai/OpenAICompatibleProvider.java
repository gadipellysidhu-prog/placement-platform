package com.college.placement.modules.jobintelligence.ai;

import com.college.placement.modules.jobintelligence.configuration.JobIntelligenceProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Provider for any OpenAI-compatible chat-completions endpoint. One implementation
 * covers Groq (hosted open-source models, the default), local Ollama, OpenAI, and
 * every other vendor that speaks {@code POST {baseUrl}/chat/completions} — the
 * distinction is configuration only (base-url / api-key / model).
 *
 * <p>The API key is read from configuration (environment) and never logged.
 */
@Slf4j
public class OpenAICompatibleProvider implements AIProvider {

    public static final String ID = "openai-compatible";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final JobIntelligenceProperties.Ai config;

    public OpenAICompatibleProvider(JobIntelligenceProperties.Ai config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.requestTimeout())
                .build();
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String model() {
        return config.model();
    }

    @Override
    public CompletionResult complete(String prompt) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", config.model());
        body.put("temperature", config.temperature());
        body.put("max_tokens", config.maxTokens());
        // JSON mode where supported; harmless elsewhere because the prompt demands JSON.
        body.putObject("response_format").put("type", "json_object");
        var messages = body.putArray("messages");
        messages.addObject().put("role", "user").put("content", prompt);

        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(chatCompletionsUrl()))
                .timeout(config.requestTimeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        if (config.apiKey() != null && !config.apiKey().isBlank()) {
            request.header("Authorization", "Bearer " + config.apiKey());
        }

        long start = System.currentTimeMillis();
        final HttpResponse<String> response;
        try {
            response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            throw new AIProviderException("LLM request failed: " + ex.getMessage(), true, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AIProviderException("LLM request interrupted", true, ex);
        }
        long latencyMs = System.currentTimeMillis() - start;

        int status = response.statusCode();
        if (status == 429 || status >= 500) {
            throw new AIProviderException("LLM endpoint unavailable (HTTP " + status + ")", true);
        }
        if (status != 200) {
            throw new AIProviderException("LLM endpoint rejected request (HTTP " + status + ")", false);
        }

        try {
            JsonNode root = objectMapper.readTree(response.body());
            String text = root.path("choices").path(0).path("message").path("content").asText(null);
            if (text == null || text.isBlank()) {
                throw new AIProviderException("LLM returned an empty completion", false);
            }
            int inputTokens = root.path("usage").path("prompt_tokens").asInt(0);
            int outputTokens = root.path("usage").path("completion_tokens").asInt(0);
            return new CompletionResult(text, inputTokens, outputTokens, latencyMs);
        } catch (AIProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AIProviderException("Unreadable LLM response envelope", false, ex);
        }
    }

    @Override
    public boolean healthy() {
        return config.baseUrl() != null && !config.baseUrl().isBlank();
    }

    private String chatCompletionsUrl() {
        String base = config.baseUrl();
        return base.endsWith("/") ? base + "chat/completions" : base + "/chat/completions";
    }
}
