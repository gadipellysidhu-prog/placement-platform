package com.college.placement.modules.jobintelligence.configuration;

import com.college.placement.shared.settings.service.SettingsService;
import org.springframework.stereotype.Component;

/**
 * Runtime feature flags for the AI pipeline, backed by SettingsService so an
 * administrator can toggle behaviour without a restart. Everything defaults OFF
 * except the sub-stages, which default ON once the master switch is enabled —
 * turning the master flag off restores the exact pre-AI manual workflow.
 */
@Component
public class JobIntelligenceFlags {

    public static final String ENABLED = "job.intelligence.enabled";
    public static final String AUTO_TAGGING = "job.intelligence.auto-tagging";
    public static final String CATALOG_UPDATES = "job.intelligence.catalog-updates";
    public static final String BRANCH_PREDICTION = "job.intelligence.branch-prediction";

    private final SettingsService settingsService;

    public JobIntelligenceFlags(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /** Master switch — when false the pipeline never starts and runs are rejected. */
    public boolean enabled() {
        return settingsService.getBoolean(ENABLED, false);
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
