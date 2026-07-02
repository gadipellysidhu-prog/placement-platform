package com.college.placement.shared.settings.service;

import com.college.placement.shared.audit.service.AuditService;
import com.college.placement.shared.settings.domain.AppSetting;
import com.college.placement.shared.settings.domain.SettingValueType;
import com.college.placement.shared.settings.repository.AppSettingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration-driven settings facade. Provides typed accessors with safe
 * defaults so business code never hardcodes tunable values (OTP TTLs, policy
 * defaults, provider toggles, ...). Global values are cached in-process and the
 * cache is invalidated on every upsert/delete. Year-scoped lookups fall back to
 * the global value, then to the supplied default.
 *
 * <p>Mutations are audited (without recording secret values verbatim is the
 * caller's responsibility for sensitive keys).
 */
@Slf4j
@Service
public class SettingsService {

    private static final String ENTITY = "AppSetting";

    private final AppSettingRepository repository;
    private final AuditService auditService;

    /** key -> raw value for global (year == null) settings. */
    private final ConcurrentHashMap<String, String> globalCache = new ConcurrentHashMap<>();

    public SettingsService(AppSettingRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    // ── Typed global accessors ───────────────────────────────────────────

    public String getString(String key, String defaultValue) {
        String raw = resolveGlobalRaw(key);
        return raw != null ? raw : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        String raw = resolveGlobalRaw(key);
        try {
            return raw != null ? Integer.parseInt(raw.trim()) : defaultValue;
        } catch (NumberFormatException ex) {
            log.warn("SETTING_PARSE_FAILED key={} type=INTEGER value={}", key, raw);
            return defaultValue;
        }
    }

    public long getLong(String key, long defaultValue) {
        String raw = resolveGlobalRaw(key);
        try {
            return raw != null ? Long.parseLong(raw.trim()) : defaultValue;
        } catch (NumberFormatException ex) {
            log.warn("SETTING_PARSE_FAILED key={} type=LONG value={}", key, raw);
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String raw = resolveGlobalRaw(key);
        return raw != null ? Boolean.parseBoolean(raw.trim()) : defaultValue;
    }

    public BigDecimal getDecimal(String key, BigDecimal defaultValue) {
        String raw = resolveGlobalRaw(key);
        try {
            return raw != null ? new BigDecimal(raw.trim()) : defaultValue;
        } catch (NumberFormatException ex) {
            log.warn("SETTING_PARSE_FAILED key={} type=DECIMAL value={}", key, raw);
            return defaultValue;
        }
    }

    // ── Year-scoped accessors (fall back to global, then default) ────────

    public String getString(String key, UUID academicYearId, String defaultValue) {
        if (academicYearId == null) {
            return getString(key, defaultValue);
        }
        return repository.findBySettingKeyAndAcademicYearId(key, academicYearId)
                .map(AppSetting::getSettingValue)
                .filter(v -> v != null && !v.isBlank())
                .orElseGet(() -> getString(key, defaultValue));
    }

    // ── Mutation ─────────────────────────────────────────────────────────

    @Transactional
    public AppSetting upsert(String key, String value, SettingValueType type,
                             String category, String description, UUID academicYearId) {
        validateValue(value, type);
        Optional<AppSetting> existing = (academicYearId == null)
                ? repository.findBySettingKeyAndAcademicYearIdIsNull(key)
                : repository.findBySettingKeyAndAcademicYearId(key, academicYearId);

        AppSetting setting = existing.orElseGet(AppSetting::new);
        String previous = setting.getSettingValue();
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        setting.setValueType(type);
        setting.setCategory(category);
        setting.setDescription(description);
        setting.setAcademicYearId(academicYearId);
        AppSetting saved = repository.save(setting);

        if (academicYearId == null) {
            globalCache.put(key, value == null ? "" : value);
        }
        auditService.record(ENTITY, saved.getId().toString(),
                existing.isPresent() ? "SETTING_UPDATED" : "SETTING_CREATED",
                previous, value, "key=" + key);
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        AppSetting setting = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Setting not found"));
        repository.delete(setting);
        if (setting.getAcademicYearId() == null) {
            globalCache.remove(setting.getSettingKey());
        }
        auditService.record(ENTITY, id, "SETTING_DELETED");
    }

    @Transactional(readOnly = true)
    public Page<AppSetting> list(String category, Pageable pageable) {
        return (category == null || category.isBlank())
                ? repository.findAll(pageable)
                : repository.findByCategory(category, pageable);
    }

    @Transactional(readOnly = true)
    public AppSetting getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Setting not found"));
    }

    /** Clears the in-process cache; useful after bulk external changes. */
    public void evictCache() {
        globalCache.clear();
    }

    // ── Internals ────────────────────────────────────────────────────────

    private String resolveGlobalRaw(String key) {
        String cached = globalCache.get(key);
        if (cached != null) {
            return cached.isEmpty() ? null : cached;
        }
        String value = repository.findBySettingKeyAndAcademicYearIdIsNull(key)
                .map(AppSetting::getSettingValue)
                .orElse(null);
        // Cache misses as empty string to avoid repeated DB hits for unset keys.
        globalCache.put(key, value == null ? "" : value);
        return (value != null && !value.isBlank()) ? value : null;
    }

    private void validateValue(String value, SettingValueType type) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            switch (type) {
                case INTEGER -> Integer.parseInt(value.trim());
                case LONG -> Long.parseLong(value.trim());
                case BOOLEAN -> {
                    String v = value.trim().toLowerCase();
                    if (!v.equals("true") && !v.equals("false")) {
                        throw new NumberFormatException("not a boolean");
                    }
                }
                case DECIMAL -> new BigDecimal(value.trim());
                case STRING, JSON -> { /* no structural validation */ }
            }
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Value does not match declared type " + type);
        }
    }
}
