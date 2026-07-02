package com.college.placement.modules.auth.domain;

/**
 * The purpose of a {@link VerificationToken}. The subsystem is generic: new
 * verification workflows can be supported by adding a type here without changing
 * the token generation, hashing, validation, consumption or cleanup logic.
 */
public enum VerificationTokenType {
    EMAIL_VERIFICATION,
    PASSWORD_RESET,
    USER_INVITATION,
    MFA_VERIFICATION,
    EMAIL_CHANGE
}
