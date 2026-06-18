/**
 * Placement module: applications, recruitment rounds, and offers.
 *
 * <p>Orchestrates the core flow. May depend on the public APIs of student, company, eligibility,
 * policy, notification (via event bus), and the outbox. Never on another module's internals (MOP 6.3).
 */
package com.college.placement.modules.placement;
