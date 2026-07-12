package com.college.placement.modules.aigovernance.service;

import com.college.placement.modules.aigovernance.domain.AIModel;
import com.college.placement.modules.aigovernance.domain.InferenceHistory;
import com.college.placement.modules.aigovernance.domain.PromptRegistry;
import com.college.placement.modules.aigovernance.repository.AIModelRepository;
import com.college.placement.modules.aigovernance.repository.InferenceHistoryRepository;
import com.college.placement.modules.aigovernance.repository.PromptRegistryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class AIGovernanceService {

    private final AIModelRepository aiModelRepository;
    private final PromptRegistryRepository promptRegistryRepository;
    private final InferenceHistoryRepository inferenceHistoryRepository;

    public AIGovernanceService(AIModelRepository aiModelRepository,
                               PromptRegistryRepository promptRegistryRepository,
                               InferenceHistoryRepository inferenceHistoryRepository) {
        this.aiModelRepository = aiModelRepository;
        this.promptRegistryRepository = promptRegistryRepository;
        this.inferenceHistoryRepository = inferenceHistoryRepository;
    }

    // ── AIModel ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AIModel getModelById(UUID id) {
        return aiModelRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI model not found"));
    }

    @Transactional(readOnly = true)
    public AIModel getModelByName(String name) {
        return aiModelRepository.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI model not found: " + name));
    }

    @Transactional(readOnly = true)
    public List<AIModel> getEnabledModels() {
        return aiModelRepository.findByEnabledTrue();
    }

    /**
     * Idempotent lookup-or-register of a model catalog row. Used by callers (e.g. the
     * Job Intelligence pipeline) whose model identity comes from configuration, so the
     * governance ledger always has a row to attach inference history to.
     */
    @Transactional
    public AIModel ensureModel(String name, String provider, String modelVersion) {
        return aiModelRepository.findByName(name).orElseGet(() -> {
            AIModel model = new AIModel();
            model.setName(name);
            model.setProvider(provider);
            model.setModelVersion(modelVersion);
            model.setEnabled(true);
            return aiModelRepository.save(model);
        });
    }

    // ── PromptRegistry ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PromptRegistry getPromptByKey(String promptKey) {
        return promptRegistryRepository.findByPromptKey(promptKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prompt not found: " + promptKey));
    }

    @Transactional(readOnly = true)
    public List<PromptRegistry> getPromptsByModel(UUID modelId) {
        AIModel model = getModelById(modelId);
        return promptRegistryRepository.findByAiModel(model);
    }

    // ── InferenceHistory ─────────────────────────────────────────────────────

    @Transactional
    public InferenceHistory recordInference(UUID modelId, UUID promptRegistryId,
                                            int inputTokens, int outputTokens, long latencyMs,
                                            String requestedBy, boolean success, String errorMessage) {
        AIModel model = getModelById(modelId);
        InferenceHistory history = new InferenceHistory();
        history.setAiModel(model);
        if (promptRegistryId != null) {
            PromptRegistry prompt = promptRegistryRepository.findById(promptRegistryId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prompt registry entry not found"));
            history.setPromptRegistry(prompt);
        }
        history.setInputTokens(inputTokens);
        history.setOutputTokens(outputTokens);
        history.setLatencyMs(latencyMs);
        history.setRequestedBy(requestedBy);
        history.setSuccess(success);
        history.setErrorMessage(errorMessage);
        return inferenceHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public Page<InferenceHistory> getInferencesByModel(UUID modelId, Pageable pageable) {
        AIModel model = getModelById(modelId);
        return inferenceHistoryRepository.findByAiModel(model, pageable);
    }

    @Transactional(readOnly = true)
    public Page<InferenceHistory> getFailedInferences(Pageable pageable) {
        return inferenceHistoryRepository.findBySuccessFalse(pageable);
    }
}
