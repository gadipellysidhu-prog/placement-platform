package com.college.placement.modules.jobintelligence.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * The structured JSON contract the LLM must return — mirrors the specification
 * exactly. All fields optional at parse time; {@code ExtractionValidator} enforces
 * semantic constraints and sanitizes every string before the data is stored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtractedJobData(
        String title,
        List<String> requiredSkills,
        List<String> preferredSkills,
        String experience,
        String education,
        List<String> technologies,
        List<String> frameworks,
        List<String> cloudPlatforms,
        List<String> softSkills,
        String salary,
        String location,
        String employmentType,
        List<String> responsibilities,
        List<String> qualifications,
        /* 0-100 self-reported extraction confidence; optional. */
        Double confidence
) {
    /** Every skill-bearing array flattened in priority order (required first). */
    public List<String> allSkillMentions() {
        java.util.ArrayList<String> all = new java.util.ArrayList<>();
        for (List<String> group : java.util.Arrays.asList(
                requiredSkills, technologies, frameworks, cloudPlatforms, preferredSkills, softSkills)) {
            if (group != null) {
                all.addAll(group);
            }
        }
        return all;
    }
}
