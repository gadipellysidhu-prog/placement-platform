/**
 * Student module: student profiles, self-registration (OTP), and profile building.
 *
 * <p>Bounded context per SADD 2.4. May depend on {@code shared/*}, {@code eligibility.api},
 * and {@code notification.api} via the internal event bus only. Must NOT reach into the internal
 * implementation of company, placement, or certificate (MOP 6.3).
 */
package com.college.placement.modules.student;
