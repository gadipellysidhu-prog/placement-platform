package com.college.placement.modules.analytics.dto;

import java.util.Map;

/**
 * The application pipeline broken down by status, plus the grand total.
 *
 * @param byStatus count keyed by {@code ApplicationStatus} name; every status is
 *                 present (zero-filled) so the funnel renders consistently
 * @param total    sum of all application counts
 */
public record ApplicationFunnelResponse(Map<String, Long> byStatus, long total) {
}
