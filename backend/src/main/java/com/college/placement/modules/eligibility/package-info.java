/**
 * Eligibility module: the deterministic eligibility engine.
 *
 * <p>Deterministic core (SADD 1.2, MOP 3.2): no randomness, no external calls, no time-dependent
 * logic. Given the same inputs it must always produce the same result. May depend on {@code shared/*}
 * utilities ONLY -- no other business module (MOP 6.3).
 */
package com.college.placement.modules.eligibility;
