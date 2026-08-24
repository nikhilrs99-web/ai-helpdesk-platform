# ADR 0003: Transactional Outbox Pattern for Kafka event publishing

**Status**: Accepted

## Context
Publishing a Kafka event right after a database write (e.g., after saving a new Ticket) is not atomic: the DB commit can succeed while the Kafka publish fails (or vice versa), leaving downstream consumers (notification-service, analytics-service) out of sync with the source of truth.

## Decision
Use the **Transactional Outbox Pattern**: when ticket-service writes a Ticket, it writes a corresponding row to an `outbox` table in the **same database transaction**. A separate outbox worker polls the `outbox` table, publishes unpublished rows to Kafka, and marks them as sent once the broker acknowledges.

## Consequences
- Ticket writes and their events are guaranteed to either both happen or neither happens — no dual-write inconsistency.
- Adds an `outbox` table and a polling worker component.
- Sets up natural support for idempotent consumers later, since each outbox row carries a unique event ID consumers can dedupe against.
