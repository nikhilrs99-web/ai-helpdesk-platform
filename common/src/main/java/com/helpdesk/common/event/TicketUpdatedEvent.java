package com.helpdesk.common.event;

import com.helpdesk.common.enums.TicketStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a ticket's status changes. Version 1 fields only - see
 * docs/kafka/event-schema.md for the versioning policy before adding, removing, or
 * renaming a field.
 */
public record TicketUpdatedEvent(
        UUID eventId,
        int version,
        Instant occurredAt,
        UUID ticketId,
        TicketStatus previousStatus,
        TicketStatus newStatus
) implements DomainEvent {

    public static final int CURRENT_VERSION = 1;
    public static final String EVENT_TYPE = "ticket.updated";

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }
}
