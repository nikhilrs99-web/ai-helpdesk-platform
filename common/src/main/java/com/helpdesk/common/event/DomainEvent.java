package com.helpdesk.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Common shape for every event this platform publishes. Lets the outbox worker (Week 6)
 * serialize and publish any event generically instead of needing a special case per type.
 */
public interface DomainEvent {

    UUID eventId();

    int version();

    Instant occurredAt();

    String eventType();
}
