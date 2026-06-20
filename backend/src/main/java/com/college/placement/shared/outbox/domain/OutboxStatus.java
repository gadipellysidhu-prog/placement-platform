package com.college.placement.shared.outbox.domain;

public enum OutboxStatus {
    PENDING, PROCESSING, SENT, FAILED, DEAD
}
