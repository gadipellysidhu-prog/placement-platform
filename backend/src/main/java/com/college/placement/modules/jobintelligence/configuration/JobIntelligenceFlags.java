package com.college.placement.modules.jobintelligence.configuration;

import com.college.placement.shared.settings.service.SettingsService;
import org.springframework.stereotype.Component;

/**
 * Runtime feature flags for the AI pipeline, backed by SettingsService so an
 * administrator can toggle behaviour without a restart. Everything defaults OFF
 * except the sub-stages, which default ON once the master switch is enabled —
 * turning the master flag off restores the exact pre-AI manual workflow.
 *
 * <p>The master switch's <em>default</em> comes from the deploy-time static
 * configuration ({@link JobIntelligenceProperties#enabled()}, i.e. the
 * {@code job.intelligence.enabled} property in {@code application*.yml} / env), so a
 * fresh database with no persisted setting still honours the configured state. A
 * persisted {@code app_settings} row (set by an administrator via SettingsService)
 * always overrides that default — preserving the no-restart runtime kill-switch.
 */
@Component
public class JobIntelligenceFlags {

    public static final String ENABLED = "job.intelligence.enabled";
    public static final String AUTO_TAGGING = "job.intelligence.auto-tagging";
    public static final String CATALOG_UPDATES = "job.intelligence.catalog-updates";
    public static final String BRANCH_PREDICTION = "job.intelligence.branch-prediction";

    private final SettingsService settingsService;
    private final JobIntelligenceProperties properties;

    public JobIntelligenceFlags(SettingsService settingsService, JobIntelligenceProperties properties) {
        this.settingsService = settingsService;
        this.properties = properties;
    }

    /**
     * Master switch — when false the pipeline never starts and runs are rejected.
     * Defaults to the configured {@code job.intelligence.enabled} property; a persisted
     * runtime setting (if any) takes precedence.
     */
    public boolean enabled() {
        return settingsService.getBoolean(ENABLED, properties.enabled());
    }

    /** Attach matched skills to the posting automatically. */
    public boolean autoTagging() {
        return settingsService.getBoolean(AUTO_TAGGING, true);
    }

    /** Create new catalog skills for unknown extracted skills. */
    public boolean catalogUpdates() {
        return settingsService.getBoolean(CATALOG_UPDATES, true);
    }

    /** Predict and attach eligible branches. */
    public boolean branchPrediction() {
        return settingsService.getBoolean(BRANCH_PREDICTION, true);
    }
}
