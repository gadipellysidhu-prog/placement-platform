package com.college.placement.shared.outbox.handler;

import com.college.placement.shared.outbox.domain.OutboxEvent;

public interface OutboxEventHandler {

    void handle(OutboxEvent event);
}
