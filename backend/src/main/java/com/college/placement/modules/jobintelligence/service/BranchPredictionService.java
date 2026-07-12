package com.college.placement.modules.jobintelligence.service;

import com.college.placement.modules.student.domain.Branch;
import com.college.placement.modules.student.service.BranchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Predicts eligible engineering branches from extracted skill mentions using a
 * deterministic keyword→branch rule table. Only branches that actually exist in
 * the institution's Branch catalog are suggested (matched by name or code,
 * case-insensitive) — the AI never invents branches. Officers can override
 * everything afterwards.
 */
@Service
public class BranchPredictionService {

    /** Skill keyword (lower-case, substring match) → candidate branch names/codes. */
    private static final Map<String, List<String>> RULES = buildRules();

    private final BranchService branchService;

    public BranchPredictionService(BranchService branchService) {
        this.branchService = branchService;
    }

    /**
     * @return prediction with the existing catalog branches to attach plus the raw
     *         predicted names (for display even when no matching Branch exists).
     */
    @Transactional(readOnly = true)
    public Prediction predict(List<String> skillMentions) {
        Set<String> predictedNames = new LinkedHashSet<>();
        for (String mention : skillMentions) {
            String needle = mention.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, List<String>> rule : RULES.entrySet()) {
                if (needle.contains(rule.getKey())) {
                    predictedNames.addAll(rule.getValue());
                }
            }
        }
        if (predictedNames.isEmpty()) {
            // Software-flavoured default: postings with no domain signals are
            // overwhelmingly software roles open to CS/IT.
            predictedNames.add("Computer Science");
            predictedNames.add("Information Technology");
        }

        List<Branch> catalog = branchService.getAll();
        Set<Branch> matched = new LinkedHashSet<>();
        for (String predicted : predictedNames) {
            String normalized = predicted.toLowerCase(Locale.ROOT);
            catalog.stream()
                    .filter(Branch::isActive)
                    .filter(b -> b.getName().toLowerCase(Locale.ROOT).contains(normalized)
                            || normalized.contains(b.getName().toLowerCase(Locale.ROOT))
                            || (b.getCode() != null && b.getCode().equalsIgnoreCase(predicted)))
                    .forEach(matched::add);
        }
        return new Prediction(List.copyOf(matched), List.copyOf(predictedNames));
    }

    private static Map<String, List<String>> buildRules() {
        Map<String, List<String>> rules = new LinkedHashMap<>();
        // Embedded / electronics signals
        for (String kw : List.of("embedded", "stm32", "uart", "spi", "i2c", "can", "rtos",
                "firmware", "microcontroller", "arm", "fpga", "vhdl", "verilog", "vlsi", "pcb",
                "arduino", "esp32", "8051", "signal processing", "dsp")) {
            rules.put(kw, List.of("Electronics & Communication", "ECE", "Electrical",
                    "Electronics", "Embedded Systems", "Computer Science"));
        }
        // Electrical / power signals
        for (String kw : List.of("power electronics", "power systems", "plc", "scada",
                "switchgear", "electrical machines", "renewable")) {
            rules.put(kw, List.of("Electrical", "EEE", "Electrical & Electronics"));
        }
        // Mechanical signals
        for (String kw : List.of("solidworks", "catia", "autocad", "ansys", "cnc", "hvac",
                "thermodynamics", "manufacturing", "mechanical design", "gd&t")) {
            rules.put(kw, List.of("Mechanical", "Mechanical Engineering"));
        }
        // Civil signals
        for (String kw : List.of("staad", "etabs", "revit", "bim", "structural",
                "construction", "surveying", "geotechnical")) {
            rules.put(kw, List.of("Civil", "Civil Engineering"));
        }
        // Chemical signals
        for (String kw : List.of("hysys", "process engineering", "distillation", "chemical process")) {
            rules.put(kw, List.of("Chemical", "Chemical Engineering"));
        }
        // Software signals (broad)
        for (String kw : List.of("java", "python", "javascript", "react", "spring", "node",
                "sql", "aws", "docker", "kubernetes", "machine learning", "data", "cloud",
                "backend", "frontend", "full stack", "devops", "android", "ios")) {
            rules.put(kw, List.of("Computer Science", "CSE", "Information Technology", "IT"));
        }
        return Map.copyOf(rules);
    }

    /** Branches to attach (existing catalog rows) + raw predicted names for display. */
    public record Prediction(List<Branch> matchedBranches, List<String> predictedNames) {}
}
