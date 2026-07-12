package com.college.placement.modules.jobintelligence.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Deterministic offline provider: scans the page text for a fixed vocabulary of
 * well-known skills and returns spec-shaped JSON. Powers every automated test and
 * lets the whole pipeline run without network access or an API key
 * (job.intelligence.ai.provider=stub — also the safe default).
 */
public class StubAIProvider implements AIProvider {

    public static final String ID = "stub";

    /** Recognizable tokens → canonical mention emitted in requiredSkills. */
    private static final List<String> VOCABULARY = List.of(
            "Java", "Python", "JavaScript", "TypeScript", "C++", "Go", "SQL",
            "Spring Boot", "React", "Angular", "Node.js", "Django",
            "Docker", "Kubernetes", "Jenkins", "Terraform", "Linux", "Git",
            "AWS", "Azure", "GCP", "PostgreSQL", "MySQL", "MongoDB", "Redis", "Kafka",
            "Machine Learning", "Deep Learning", "NLP", "TensorFlow", "PyTorch",
            "Embedded C", "STM32", "UART", "SPI", "I2C", "CAN", "FreeRTOS", "ARM",
            "Communication", "Leadership", "Teamwork", "Problem Solving"
    );

    private final ObjectMapper objectMapper;

    public StubAIProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String model() {
        return "stub-keyword-scan";
    }

    @Override
    public CompletionResult complete(String prompt) {
        String haystack = prompt.toLowerCase(Locale.ROOT);
        Set<String> found = new LinkedHashSet<>();
        for (String term : VOCABULARY) {
            if (haystack.contains(term.toLowerCase(Locale.ROOT))) {
                found.add(term);
            }
        }

        ObjectNode json = objectMapper.createObjectNode();
        json.put("title", firstLineTitle(prompt));
        ArrayNode required = json.putArray("requiredSkills");
        found.forEach(required::add);
        json.putArray("preferredSkills");
        json.put("experience", "");
        json.put("education", "");
        json.putArray("technologies");
        json.putArray("frameworks");
        json.putArray("cloudPlatforms");
        json.putArray("softSkills");
        json.put("salary", "");
        json.put("location", "");
        json.put("employmentType", "");
        json.putArray("responsibilities");
        json.putArray("qualifications");
        json.put("confidence", found.isEmpty() ? 10.0 : 90.0);
        return new CompletionResult(json.toString(), prompt.length() / 4, 200, 5);
    }

    @Override
    public boolean healthy() {
        return true;
    }

    private static String firstLineTitle(String prompt) {
        int marker = prompt.indexOf("PAGE TEXT:");
        String text = marker >= 0 ? prompt.substring(marker + 10) : prompt;
        String trimmed = text.strip();
        int end = Math.min(trimmed.length(), 80);
        return trimmed.isEmpty() ? "Imported Job" : trimmed.substring(0, end);
    }
}
