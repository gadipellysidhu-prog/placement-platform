/**
 * AI Governance module: model registry, prompt registry, and inference history.
 *
 * <p>Must remain generic: no dependency on business modules (MOP 6.3). Every AI inference is
 * recorded with status REVIEW_NEEDED for human-in-the-loop review (SADD 7, MOP 3.3).
 */
package com.college.placement.modules.aigovernance;
