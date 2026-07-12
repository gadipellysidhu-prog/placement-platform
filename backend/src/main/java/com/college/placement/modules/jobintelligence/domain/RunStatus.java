package com.college.placement.modules.jobintelligence.domain;

/**
 * Lifecycle of an AI extraction run. Progress states advance in order and are
 * surfaced live to the frontend; COMPLETED and FAILED are terminal.
 */
public enum RunStatus {
    PENDING,
    FETCHING,
    EXTRACTING,
    NORMALIZING,
    TAGGING,
    PREDICTING_BRANCHES,
    COMPLETED,
    FAILED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}
