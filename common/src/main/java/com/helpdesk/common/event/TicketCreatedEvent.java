package com.helpdesk.common.event;

import com.helpdesk.common.enums.TicketCategory;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a new ticket is created. Version 1 fields only - see
 * docs/kafka/event-schema.md for the versioning policy before adding, removing, or
 * renaming a field.
 */
public record TicketCreatedEvent(
        UUID eventId,
        int version,
        Instant occurredAt,
        UUID ticketId,
        TicketCategory category,
        String requesterId
) implements DomainEvent {

    public static final int CURRENT_VERSION = 1;
    public static final String EVENT_TYPE = "ticket.created";

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }
}
