package com.helpdesk.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a ticket misses its SLA target. Version 1 fields only - see
 * docs/kafka/event-schema.md for the versioning policy before adding, removing, or
 * renaming a field.
 */
// eventType is serialized (see below) but isn't a record component, so a consumer
// deserializing this same JSON back into a SlaBreachedEvent would otherwise fail with
// UnrecognizedPropertyException on its own eventType field.
@JsonIgnoreProperties(ignoreUnknown = true)
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

    // Without @JsonProperty here, eventType() isn't a record component, so Jackson would
    // drop it from the serialized JSON entirely - every consumer (notification-service,
    // analytics-service) reads this field by name to route the event.
    @Override
    @JsonProperty("eventType")
    public String eventType() {
        return EVENT_TYPE;
    }
}
