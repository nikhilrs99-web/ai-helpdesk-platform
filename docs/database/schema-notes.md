# Database Schema Notes

## Entities (ticket-service, Day 8)
- **Ticket** — subject, description, category (`TicketCategory`), status (`TicketStatus`, defaults
  `OPEN`), requesterId (Keycloak subject), optional assignedAgent (FK to Agent), routedTeam (Day 11,
  Strategy pattern), metadata (Day 12, Factory pattern — see below)
- **TicketComment** — belongs to a Ticket, authorId, body, aiDrafted flag
- **Agent** — keycloakSubjectId (unique), displayName, team
- **Sla** — SLA targets by (category, slaType), e.g. (BILLING, FIRST_RESPONSE, 240 minutes). A
  reference/config table for now; live breach tracking against these targets is a Kafka-driven feature
  added later (`sla.breached`, see `docs/kafka/event-schema.md`)

All four share `id` (UUID), `createdAt`, `updatedAt` via a `BaseEntity` `@MappedSuperclass`, populated
automatically by Spring Data JPA auditing rather than set manually anywhere.

`TicketCategory` and `TicketStatus` are **not** redefined here — they're the enums already in `common`
(Day 5), so ticket-service, analytics-service, and notification-service share one definition of what
these values mean.

No `User`/`Customer` entity exists here deliberately — identity is owned by Keycloak (ADR-0001); a
ticket only stores a Keycloak subject string, not a local copy of the user.

## `ticket_metadata` (Day 12)
`Ticket.metadata` (`Map<String, String>`) is mapped via JPA's `@ElementCollection`/`@CollectionTable` to
a side table, not a JSON column: `ticket_metadata(ticket_id, meta_key, meta_value)`, with a composite
primary key on `(ticket_id, meta_key)` that Hibernate derives automatically. Which keys are required
depends on the ticket's category (BUG needs `browser`/`appVersion`, BILLING needs `invoiceId`) — enforced
in code by the matching `TicketTypeHandler`, not by a database constraint, since the requirement varies
per row rather than being a fixed schema rule.

**Bug found and fixed Day 13**: `@ElementCollection` defaults to `FetchType.LAZY`, which is normally
fine, but `spring.jpa.open-in-view` is deliberately `false` here (see below) — so once a repository call
returns, the Hibernate session that could lazily initialize `metadata` is already closed. `create()`
never hit this (it serializes a brand-new, never-persisted `Ticket` whose `metadata` is a plain
`HashMap`, not a Hibernate-managed lazy proxy), but `GET`/`PUT`/`PATCH .../status` all load the ticket
via `findById` first — every one of them 500'd with `LazyInitializationException` wrapped as
`HttpMessageNotWritableException` the first time they were exercised end to end against a real ticket.
This had been latent since Day 12 (nothing before Day 13 actually called `GET`/`PUT`/`PATCH` against a
ticket that has JPA-managed metadata) and only surfaced during Day 13's authorization verification.
Fixed by marking the mapping `@ElementCollection(fetch = FetchType.EAGER)` — reasonable here since
`TicketResponse` always serializes `metadata` anyway and it's at most a couple of key/value pairs, so
there's no N+1 concern worth trading away simplicity for.

## Why no Flyway/Liquibase yet
`spring.jpa.hibernate.ddl-auto: update` is used for now, deliberately, so the entity shape can keep
changing freely while the domain model is still settling (through the rest of Week 2/3). Versioned
migrations are planned once the shape stabilizes, per the original build plan. This is a sequencing
choice, not an oversight — `ddl-auto: update` should never be used once real data exists that a
migration tool needs to safely evolve around.

## Verification
The schema was verified directly against a real (non-mocked) PostgreSQL instance two ways:
1. Started ticket-service against the actual `helpdesk-postgres` container and inspected the tables
   Hibernate generated with `psql \dt` / `\d <table>` — confirmed columns, foreign keys, and even the
   CHECK constraints Hibernate derives automatically from the `TicketCategory`/`TicketStatus` enums.
2. A Testcontainers-based integration test (`TicketRepositoryIT`) that saves an Agent and a Ticket
   through the real repositories and asserts the auditing fields and the FK relationship are populated
   correctly — written using the standard `*IT.java` + Failsafe convention (`mvn verify`), not `mvn
   test`, since it's a slower integration test against real infrastructure, not a unit test.

**Known local limitation**: on this Windows machine, `TicketRepositoryIT` cannot currently execute —
Testcontainers' Docker client gets a malformed/stub response from Docker Desktop's Engine API over the
Windows named pipe, even though the `docker` CLI itself works perfectly for everything else in this
project (Postgres, Keycloak). Tried and ruled out: multiple named pipes, `DOCKER_HOST` overrides, a full
OS restart, and a newer Testcontainers version (1.20.4) — all hit the identical stub response. This
looks like a deeper Docker-Desktop-on-Windows/docker-java npipe compatibility issue, not a bug in this
project's code. The test itself is correctly written and will run normally in the GitHub Actions CI
pipeline (Linux runners don't have this npipe layer at all) once that's set up in Phase 13. Verification
method (1) above is the authoritative proof of the schema until then.
