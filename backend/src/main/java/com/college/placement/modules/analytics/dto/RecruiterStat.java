package com.college.placement.modules.analytics.dto;

/**
 * Recruiting activity for a single company, ranked by number of offers made.
 *
 * @param company           company name
 * @param offers            applications to this company's postings in {@code OFFERED} state
 * @param totalApplications all applications received across this company's postings
 */
public record RecruiterStat(String company, long offers, long totalApplications) {
}
