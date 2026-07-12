package com.college.placement.modules.jobintelligence.dto;

import com.college.placement.modules.jobintelligence.domain.JobIntelligenceRun;
import com.college.placement.modules.jobintelligence.domain.RunStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "AI extraction run status/result")
public record RunResponse(
        UUID id,
        UUID jobPostingId,
        String officialUrl,
        RunStatus status,
        boolean terminal,
        String provider,
        String model,
        BigDecimal confidence,
        int skillsExtracted,
        int skillsCreated,
        int skillsTagged,
        List<String> predictedBranches,
        List<String> warnings,
        String errorMessage,
        int retryCount,
        Instant startedAt,
        Instant completedAt,
        Long durationMs,
        Instant createdAt
) {
    public static RunResponse from(JobIntelligenceRun run, ObjectMapper objectMapper) {
        return new RunResponse(
                run.getId(),
                run.getJobPostingId(),
                run.getOfficialUrl(),
                run.getStatus(),
                run.getStatus().isTerminal(),
                run.getProvider(),
                run.getModel(),
                run.getConfidence(),
                run.getSkillsExtracted(),
                run.getSkillsCreated(),
                run.getSkillsTagged(),
                splitBranches(run.getPredictedBranches()),
                parseWarnings(run.getWarningsJson(), objectMapper),
                run.getErrorMessage(),
                run.getRetryCount(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getDurationMs(),
                run.getCreatedAt());
    }

    private static List<String> splitBranches(String joined) {
        if (joined == null || joined.isBlank()) {
            return List.of();
        }
        return List.of(joined.split(",\\s*"));
    }

    private static List<String> parseWarnings(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }
}
