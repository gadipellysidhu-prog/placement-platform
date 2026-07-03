package com.college.placement.modules.analytics.dto;

/**
 * One month in the application/offer time series, bucketed by application date (UTC).
 *
 * @param month        ISO year-month, e.g. {@code "2026-07"}
 * @param applications applications submitted in the month
 * @param offers       of those applications, how many are now in {@code OFFERED} state
 */
public record TrendPoint(String month, long applications, long offers) {
}
