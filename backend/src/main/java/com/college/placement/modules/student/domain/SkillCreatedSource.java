package com.college.placement.modules.student.domain;

/** How a catalog skill came into existence — used for provenance and catalog quality jobs. */
public enum SkillCreatedSource {
    /** Seeded by the initial Master Skills Catalog migration. */
    SEED,
    /** Created manually by a placement officer. */
    MANUAL,
    /** Discovered and created automatically by the AI extraction pipeline. */
    AI
}
