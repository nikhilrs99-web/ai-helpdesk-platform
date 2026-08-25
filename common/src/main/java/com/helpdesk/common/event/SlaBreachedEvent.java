package com.helpdesk.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a ticket misses its SLA target. Version 1 fields only - see
 * docs/kafka/event-schema.md for the versioning policy before adding, removing, or
 * renaming a field.
 */
public record SlaBreachedEvent(
        UUID eventId,
        int version,
        Instant occurredAt,
        UUID ticketId,
        String slaType,
        Instant breachedAt
) implements DomainEvent {

    public static final int CURRENT_VERSION = 1;
    public static final String EVENT_TYPE = "sla.breached";

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }
}
