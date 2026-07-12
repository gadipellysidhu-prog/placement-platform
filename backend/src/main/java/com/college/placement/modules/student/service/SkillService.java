package com.college.placement.modules.student.service;

import com.college.placement.modules.student.domain.Skill;
import com.college.placement.modules.student.domain.SkillAlias;
import com.college.placement.modules.student.domain.SkillCreatedSource;
import com.college.placement.modules.student.repository.SkillAliasRepository;
import com.college.placement.modules.student.repository.SkillRepository;
import com.college.placement.modules.student.service.matching.SkillSimilarityMatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class SkillService {

    private final SkillRepository skillRepository;
    private final SkillAliasRepository skillAliasRepository;
    private final SkillNormalizationService normalizationService;
    private final SkillSimilarityMatcher similarityMatcher;

    public SkillService(SkillRepository skillRepository,
                        SkillAliasRepository skillAliasRepository,
                        SkillNormalizationService normalizationService,
                        SkillSimilarityMatcher similarityMatcher) {
        this.skillRepository = skillRepository;
        this.skillAliasRepository = skillAliasRepository;
        this.normalizationService = normalizationService;
        this.similarityMatcher = similarityMatcher;
    }

    @Transactional
    public Skill createSkill(String name, String category) {
        if (skillRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Skill already exists");
        }
        Skill skill = new Skill();
        skill.setName(name);
        skill.setCategory(category);
        return skillRepository.save(skill);
    }

    @Transactional
    public Skill verify(UUID id) {
        Skill skill = getById(id);
        skill.setVerified(true);
        return skillRepository.save(skill);
    }

    @Transactional
    public Skill updateSkill(UUID id, String name, String category) {
        Skill skill = getById(id);
        if (!skill.getName().equals(name) && skillRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Skill name already exists");
        }
        skill.setName(name);
        skill.setCategory(category);
        return skillRepository.save(skill);
    }

    @Transactional(readOnly = true)
    public Skill getById(UUID id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill not found"));
    }

    @Transactional(readOnly = true)
    public List<Skill> getAll() {
        return skillRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Skill> getByCategory(String category) {
        return skillRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public List<Skill> getVerified() {
        return skillRepository.findByVerifiedTrue();
    }

    // ── Master Skills Catalog operations (Phase A) ──────────────────────────

    /**
     * Resolve a raw skill string to an existing catalog skill (via the normalization
     * engine) or create a new catalog entry. Never creates duplicates: exact names,
     * aliases, abbreviations, and fuzzy matches all resolve to the existing skill.
     */
    @Transactional
    public FindOrCreateResult findOrCreate(String rawName, String category,
                                           SkillCreatedSource source, BigDecimal aiConfidence) {
        Optional<Skill> existing = normalizationService.resolve(rawName);
        if (existing.isPresent()) {
            return new FindOrCreateResult(existing.get(), false);
        }

        String cleanName = rawName.trim().replaceAll("\\s+", " ");
        if (cleanName.isEmpty() || cleanName.length() > 100) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Skill name must be 1-100 characters");
        }

        Skill skill = new Skill();
        skill.setName(cleanName);
        skill.setCategory(category);
        skill.setCreatedSource(source);
        skill.setAiConfidence(aiConfidence);
        Skill saved = skillRepository.save(skill);

        // Continuous learning: register cheap name variants as aliases so future
        // extractions of "Nodejs"/"node js" style forms resolve without fuzzy matching.
        for (String variant : nameVariants(cleanName)) {
            saveAliasIfFree(saved, variant);
        }
        log.info("SKILL_CATALOG event=CREATED name={} source={} category={}",
                cleanName, source, category);
        return new FindOrCreateResult(saved, true);
    }

    /** Add an alias to a skill; 409 if the alias already points elsewhere or clashes with a name. */
    @Transactional
    public SkillAlias addAlias(UUID skillId, String alias) {
        Skill skill = getById(skillId);
        String normalized = SkillNormalizationService.normalize(alias);
        if (normalized.isEmpty() || normalized.length() > 100) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Alias must be 1-100 characters");
        }
        if (skillRepository.findByNameIgnoreCase(normalized).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Alias clashes with an existing skill name");
        }
        if (skillAliasRepository.existsByAliasNormalized(normalized)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Alias already exists");
        }
        SkillAlias entity = new SkillAlias();
        entity.setSkill(skill);
        entity.setAlias(alias.trim().replaceAll("\\s+", " "));
        entity.setAliasNormalized(normalized);
        return skillAliasRepository.save(entity);
    }

    @Transactional
    public void removeAlias(UUID skillId, UUID aliasId) {
        SkillAlias alias = skillAliasRepository.findById(aliasId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alias not found"));
        if (!alias.getSkill().getId().equals(skillId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Alias not found for this skill");
        }
        skillAliasRepository.delete(alias);
    }

    @Transactional(readOnly = true)
    public List<SkillAlias> getAliases(UUID skillId) {
        getById(skillId); // 404 for unknown skill
        return skillAliasRepository.findBySkillId(skillId);
    }

    /** Popularity bump used when a skill gets tagged onto a posting. */
    @Transactional
    public void incrementPopularity(UUID skillId) {
        skillRepository.incrementPopularity(skillId);
    }

    /**
     * Intelligent catalog search, ranked (never plain-alphabetical):
     * exact name > exact alias > name/alias substring > fuzzy; popularity breaks ties.
     */
    @Transactional(readOnly = true)
    public List<SkillSearchHit> search(String query, int limit) {
        String normalized = SkillNormalizationService.normalize(query);
        if (normalized.isEmpty()) {
            return List.of();
        }
        Map<UUID, SkillSearchHit> hits = new LinkedHashMap<>();

        skillRepository.findByNameIgnoreCase(normalized)
                .filter(Skill::isActive)
                .ifPresent(s -> hits.put(s.getId(), new SkillSearchHit(s, "EXACT", 1.0)));

        skillAliasRepository.findByAliasNormalized(normalized)
                .map(SkillAlias::getSkill)
                .filter(Skill::isActive)
                .ifPresent(s -> hits.putIfAbsent(s.getId(), new SkillSearchHit(s, "ALIAS", 0.95)));

        for (Skill s : skillRepository.searchByNameContaining(normalized)) {
            hits.putIfAbsent(s.getId(), new SkillSearchHit(s, "PARTIAL", 0.70));
        }
        for (SkillAlias a : skillAliasRepository.searchByAliasContaining(normalized)) {
            hits.putIfAbsent(a.getSkill().getId(), new SkillSearchHit(a.getSkill(), "PARTIAL", 0.65));
        }
        for (SkillSimilarityMatcher.SimilarityHit fuzzy : similarityMatcher.topMatches(normalized, limit)) {
            skillRepository.findById(fuzzy.skillId())
                    .filter(Skill::isActive)
                    .ifPresent(s -> hits.putIfAbsent(s.getId(),
                            new SkillSearchHit(s, "FUZZY", fuzzy.score() * 0.6)));
        }

        return hits.values().stream()
                .sorted(Comparator.comparingDouble(SkillSearchHit::score).reversed()
                        .thenComparing(h -> h.skill().getPopularityScore(), Comparator.reverseOrder())
                        .thenComparing(h -> h.skill().getName()))
                .limit(limit)
                .toList();
    }

    /** Cheap punctuation/spacing variants of a name ("Node.js" → "nodejs", "node js"). */
    private static List<String> nameVariants(String name) {
        List<String> variants = new ArrayList<>();
        String noDots = name.replace(".", "");
        String noSpaces = name.replaceAll("\\s+", "");
        String spaced = name.replaceAll("[.\\-]", " ");
        for (String v : List.of(noDots, noSpaces, spaced)) {
            String normalized = SkillNormalizationService.normalize(v);
            if (!normalized.isEmpty()
                    && !normalized.equals(SkillNormalizationService.normalize(name))
                    && !variants.contains(v)) {
                variants.add(v);
            }
        }
        return variants;
    }

    private void saveAliasIfFree(Skill skill, String alias) {
        String normalized = SkillNormalizationService.normalize(alias);
        if (normalized.isEmpty() || normalized.length() > 100
                || skillAliasRepository.existsByAliasNormalized(normalized)
                || skillRepository.findByNameIgnoreCase(normalized).isPresent()) {
            return;
        }
        SkillAlias entity = new SkillAlias();
        entity.setSkill(skill);
        entity.setAlias(alias);
        entity.setAliasNormalized(normalized);
        skillAliasRepository.save(entity);
    }

    /** Result of {@link #findOrCreate}: the canonical skill and whether it was newly created. */
    public record FindOrCreateResult(Skill skill, boolean created) {}

    /** Ranked search hit: the skill, how it matched, and its ranking score. */
    public record SkillSearchHit(Skill skill, String matchType, double score) {}
}
