package com.college.placement.jobintelligence;

import com.college.placement.modules.jobintelligence.configuration.JobIntelligenceFlags;
import com.college.placement.modules.jobintelligence.configuration.JobIntelligenceProperties;
import com.college.placement.shared.settings.service.SettingsService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the master-switch default: {@link JobIntelligenceFlags#enabled()} must default
 * to the configured {@code job.intelligence.enabled} property (never a hardcoded value),
 * while a persisted runtime setting overrides it. A regression here silently disables the
 * whole AI pipeline on a fresh database ("AI job intelligence is currently disabled").
 */
class JobIntelligenceFlagsTest {

    private static JobIntelligenceProperties propsWithEnabled(boolean enabled) {
        // Compact constructor fills the nested config with safe defaults.
        return new JobIntelligenceProperties(enabled, null, null, null, null);
    }

    @Test
    void masterDefaultsToConfiguredPropertyWhenNoSettingPersisted() {
        SettingsService settings = mock(SettingsService.class);
        // No persisted row: SettingsService returns the supplied default verbatim.
        when(settings.getBoolean(eq(JobIntelligenceFlags.ENABLED), eq(true))).thenReturn(true);

        JobIntelligenceFlags flags = new JobIntelligenceFlags(settings, propsWithEnabled(true));

        assertThat(flags.enabled()).isTrue();
        // The default handed to SettingsService is the configured property, not a literal.
        verify(settings).getBoolean(JobIntelligenceFlags.ENABLED, true);
    }

    @Test
    void masterDefaultsOffWhenConfiguredOff() {
        SettingsService settings = mock(SettingsService.class);
        when(settings.getBoolean(eq(JobIntelligenceFlags.ENABLED), eq(false))).thenReturn(false);

        JobIntelligenceFlags flags = new JobIntelligenceFlags(settings, propsWithEnabled(false));

        assertThat(flags.enabled()).isFalse();
        verify(settings).getBoolean(JobIntelligenceFlags.ENABLED, false);
    }

    @Test
    void persistedSettingOverridesConfiguredDefault() {
        SettingsService settings = mock(SettingsService.class);
        // Configured off, but an administrator persisted "true" — the runtime value wins.
        when(settings.getBoolean(eq(JobIntelligenceFlags.ENABLED), eq(false))).thenReturn(true);

        JobIntelligenceFlags flags = new JobIntelligenceFlags(settings, propsWithEnabled(false));

        assertThat(flags.enabled()).isTrue();
    }
}
