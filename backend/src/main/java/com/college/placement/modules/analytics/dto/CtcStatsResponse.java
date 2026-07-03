package com.college.placement.modules.analytics.dto;

import java.math.BigDecimal;

/**
 * Compensation (CTC) distribution across job postings that declare a package.
 *
 * @param minCtc              lowest {@code ctcMin} among considered postings (nullable when none)
 * @param maxCtc              highest {@code ctcMax} among considered postings (nullable when none)
 * @param avgCtc             average {@code ctcMax}, two decimals (nullable when none)
 * @param postingsConsidered number of postings with a declared {@code ctcMax}
 */
public record CtcStatsResponse(
        BigDecimal minCtc,
        BigDecimal maxCtc,
        BigDecimal avgCtc,
        long postingsConsidered) {
}
