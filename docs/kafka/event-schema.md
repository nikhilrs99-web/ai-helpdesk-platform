# Kafka Event Schema

Event contracts live as Java records in the `common` module (`com.helpdesk.common.event`), so every
service depends on the same compiled definition instead of hand-copying field names. Nothing publishes
or consumes these yet — that starts in Phase 3 (Week 6) via the Transactional Outbox Pattern
(see [ADR 0003](../decisions/0003-transactional-outbox-for-kafka-publishing.md)). This document exists
now so the contract is deliberate from day one, not reverse-engineered later.

## Versioning policy
Every event carries its own `version` field (starting at 1), independent of the others.

- **Backward-compatible (no version bump needed)**: adding a new optional field that old consumers can ignore.
- **Requires a version bump**: removing a field, renaming a field, or changing a field's type/meaning.
  A version bump means old and new versions may briefly coexist on the topic — consumers must handle both
  until every producer has moved to the new version.
- Version bumps are never done by editing the existing record in place; a new record class (or a new
  field alongside the old one, deprecated) is added instead, so already-published messages of the old
  shape remain deserializable.

## Events (all version 1)

### `ticket.created` — `TicketCreatedEvent`
Published when a new ticket is created.

| Field | Type | Notes |
|---|---|---|
| `eventId` | UUID | unique per event, used by consumers for idempotent dedup |
| `version` | int | schema version of this event, currently 1 |
| `occurredAt` | Instant | when the ticket was created |
| `ticketId` | UUID | the ticket this event is about |
| `category` | TicketCategory | BUG / BILLING / ACCESS / HOW_TO / FEATURE_REQUEST |
| `requesterId` | String | Keycloak subject of the user who raised it |

### `ticket.updated` — `TicketUpdatedEvent`
Published when a ticket's status changes.

| Field | Type | Notes |
|---|---|---|
| `eventId` | UUID | |
| `version` | int | |
| `occurredAt` | Instant | |
| `ticketId` | UUID | |
| `previousStatus` | TicketStatus | |
| `newStatus` | TicketStatus | |

### `sla.breached` — `SlaBreachedEvent`
Published when a ticket misses its SLA target.

| Field | Type | Notes |
|---|---|---|
| `eventId` | UUID | |
| `version` | int | |
| `occurredAt` | Instant | |
| `ticketId` | UUID | |
| `slaType` | String | which SLA target was missed (e.g. first-response, resolution) |
| `breachedAt` | Instant | when the breach was detected |

## Partitioning (planned, Week 7)
All three topics will be partitioned by `ticketId`, so every event about the same ticket lands on the
same partition and is processed in order by a given consumer — without this, `ticket.updated` could be
processed before `ticket.created` by a fast consumer on a different partition.
