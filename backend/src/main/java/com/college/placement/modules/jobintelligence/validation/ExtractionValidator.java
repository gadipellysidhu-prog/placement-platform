package com.college.placement.modules.jobintelligence.validation;

import com.college.placement.modules.jobintelligence.dto.ExtractedJobData;
import com.college.placement.modules.jobintelligence.extractor.HtmlContentExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The trust boundary for LLM output. Parses the completion strictly as JSON,
 * enforces structural and size limits, and sanitizes every string (all HTML
 * stripped) before anything is persisted or shown to a user. Malformed output is
 * a non-retryable failure — a broken model answer will not fix itself on retry.
 */
@Component
public class ExtractionValidator {

    static final int MAX_STRING_LENGTH = 2_000;
    static final int MAX_SKILL_LENGTH = 100;
    static final int MAX_ARRAY_SIZE = 100;
    static final int MAX_LIST_TEXT_LENGTH = 500;

    private final ObjectMapper objectMapper;

    public ExtractionValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Parse + validate + sanitize; throws {@link InvalidExtractionException} on violations. */
    public ExtractedJobData validate(String rawCompletion) {
        if (rawCompletion == null || rawCompletion.isBlank()) {
            throw new InvalidExtractionException("Empty completion");
        }
        String json = stripCodeFences(rawCompletion.trim());

        final ExtractedJobData parsed;
        try {
            parsed = objectMapper.readValue(json, ExtractedJobData.class);
        } catch (Exception ex) {
            throw new InvalidExtractionException("Completion is not valid JSON: " + ex.getMessage());
        }

        if (parsed.requiredSkills() == null) {
            throw new InvalidExtractionException("requiredSkills array is missing");
        }
        Double confidence = parsed.confidence();
        if (confidence != null && (confidence < 0 || confidence > 100)) {
            confidence = null;
        }

        return new ExtractedJobData(
                text(parsed.title(), MAX_STRING_LENGTH),
                skills(parsed.requiredSkills()),
                skills(parsed.preferredSkills()),
                text(parsed.experience(), MAX_STRING_LENGTH),
                text(parsed.education(), MAX_STRING_LENGTH),
                skills(parsed.technologies()),
                skills(parsed.frameworks()),
                skills(parsed.cloudPlatforms()),
                skills(parsed.softSkills()),
                text(parsed.salary(), MAX_STRING_LENGTH),
                text(parsed.location(), MAX_STRING_LENGTH),
                text(parsed.employmentType(), MAX_STRING_LENGTH),
                texts(parsed.responsibilities()),
                texts(parsed.qualifications()),
                confidence);
    }

    /** Models often wrap JSON in ```json fences despite instructions — tolerate that only. */
    private static String stripCodeFences(String value) {
        if (value.startsWith("```")) {
            int firstNewline = value.indexOf('\n');
            int lastFence = value.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return value.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return value;
    }

    private static String text(String value, int maxLength) {
        String stripped = HtmlContentExtractor.stripHtml(value);
        if (stripped == null || stripped.isBlank()) {
            return "";
        }
        return stripped.length() > maxLength ? stripped.substring(0, maxLength) : stripped;
    }

    /** Sanitized, deduped, length-capped skill mentions; oversized entries are dropped. */
    private static List<String> skills(List<String> values) {
        if (values == null) {
            return List.of();
        }
        if (values.size() > MAX_ARRAY_SIZE) {
            values = values.subList(0, MAX_ARRAY_SIZE);
        }
        Set<String> out = new LinkedHashSet<>();
        for (String value : values) {
            String stripped = HtmlContentExtractor.stripHtml(value);
            if (stripped != null && !stripped.isBlank() && stripped.length() <= MAX_SKILL_LENGTH) {
                out.add(stripped);
            }
        }
        return List.copyOf(out);
    }

    private static List<String> texts(List<String> values) {
        if (values == null) {
            return List.of();
        }
        if (values.size() > MAX_ARRAY_SIZE) {
            values = values.subList(0, MAX_ARRAY_SIZE);
        }
        List<String> out = new ArrayList<>();
        for (String value : values) {
            String sanitized = text(value, MAX_LIST_TEXT_LENGTH);
            if (!sanitized.isEmpty()) {
                out.add(sanitized);
            }
        }
        return List.copyOf(out);
    }

    /** Non-retryable: the model produced output that violates the contract. */
    public static class InvalidExtractionException extends RuntimeException {
        public InvalidExtractionException(String message) {
            super(message);
        }
    }
}
