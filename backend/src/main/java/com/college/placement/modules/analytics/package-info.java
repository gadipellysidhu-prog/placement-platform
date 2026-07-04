/**
 * Analytics module: placement reporting and KPIs.
 *
 * <p>A read model over the operational tables — it computes aggregates (placement
 * rate, application funnel, per-branch outcomes, top recruiters, CTC distribution,
 * and application/offer trends) on demand and persists nothing. Queries are owned
 * by {@code AnalyticsRepository} (JPA aggregation, no entity hydration); the REST
 * surface under {@code /api/analytics} is restricted to placement officers and
 * administrators. Read-only and side-effect free.
 */
package com.college.placement.modules.analytics;
