package com.helpdesk.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when an agent/admin approves a PENDING escalation (see EscalationController) -
 * the point at which docs/rag/agent-tools.md says the escalation "permanently commits...
 * and triggers notifications". Version 1 fields only - see docs/kafka/event-schema.md for
 * the versioning policy before adding, removing, or renaming a field.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EscalationApprovedEvent(
        UUID eventId,
        int version,
        Instant occurredAt,
        UUID ticketId,
        UUID escalationId,
        String reason,
        String approvedBy
) implements DomainEvent {

    public static final int CURRENT_VERSION = 1;
    public static final String EVENT_TYPE = "escalation.approved";

    @Override
    @JsonProperty("eventType")
    public String eventType() {
        return EVENT_TYPE;
    }
}
