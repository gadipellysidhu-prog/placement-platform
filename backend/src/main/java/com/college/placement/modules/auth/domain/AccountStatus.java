package com.college.placement.modules.auth.domain;

/**
 * Administrative lifecycle state of an {@link AppUser}. Distinct from the
 * transient brute-force lock ({@code lockedUntil}); this is set explicitly by
 * administrators (or by the invitation flow) and gates login.
 */
public enum AccountStatus {
    /** Normal, usable account. */
    ACTIVE,
    /** Administratively deactivated (soft-disabled); cannot log in. */
    DISABLED,
    /** Administratively locked; cannot log in until unlocked. */
    LOCKED,
    /** Created via invitation, awaiting activation; cannot log in yet. */
    INVITED
}
