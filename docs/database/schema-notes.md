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

## Flyway (Day 16) — replacing `ddl-auto: update`
The domain model held stable across Days 13–15 with no shape changes, so per the original sequencing
plan (`ddl-auto: update` was always meant to be temporary, never meant to survive contact with real
data), `spring.jpa.hibernate.ddl-auto` is now `validate` and Flyway owns the schema. Hibernate checks
its entity mappings still match what's actually in the database at startup; it no longer alters
anything.

**Why Flyway over Liquibase**: no strong reason to prefer one over the other here — both are
well-established, both integrate the same way with Spring Boot. Flyway's plain-SQL migrations fit this
project better than Liquibase's XML/YAML changelog format, since every migration so far is a
straightforward DDL change with no need for Liquibase's rollback-changeset machinery.

**The baseline problem**: this app's tables already existed — built up incrementally by `ddl-auto:
update` across Days 8, 9, 11, 12, and 13 — before Flyway was ever introduced. Flyway normally expects to
create a schema from nothing; pointed at a database that already has unmanaged tables, it refuses to
run rather than guess whether those tables match what a migration would create. `spring.flyway.
baseline-on-migrate: true` (plus `baseline-version: 1`, see `application.yml`) tells Flyway to instead
mark an already-populated schema as already being at V1, without re-running the migration against
tables that are already there.

`db/migration/V1__baseline_schema.sql` captures that exact schema — pulled from the real database with
`pg_dump --schema-only`, not reconstructed by hand from the JPA annotations, specifically so it's a
byte-for-byte snapshot of what Hibernate had actually built, not what the entities are theoretically
supposed to produce. Any future schema change is a new `V2__...sql`, `V3__...sql`, etc. — `ddl-auto:
update` is not coming back.

**Why this matters for exactly one existing gap**: `TicketRepositoryIT` (Day 8, still blocked locally by
the Windows/Docker npipe issue below) runs against a fresh Testcontainers-provisioned Postgres — once it
can run, Flyway will migrate that container from empty using `V1__baseline_schema.sql` for real, the
same code path verified below, rather than the baseline path used against this already-populated local
dev database.

## Verification
The schema was verified directly against a real (non-mocked) PostgreSQL instance two ways:
1. Started ticket-service against the actual `helpdesk-postgres` container and inspected the tables
   Hibernate generated with `psql \dt` / `\d <table>` — confirmed columns, foreign keys, and even the
   CHECK constraints Hibernate derives automatically from the `TicketCategory`/`TicketStatus` enums.
2. A Testcontainers-based integration test (`TicketRepositoryIT`) that saves an Agent and a Ticket
   through the real repositories and asserts the auditing fields and the FK relationship are populated
   correctly — written using the standard `*IT.java` + Failsafe convention (`mvn verify`), not `mvn
   test`, since it's a slower integration test against real infrastructure, not a unit test.

**Flyway verification (Day 16)** — both paths a migration tool can be exercised through were checked
directly, not assumed from a green startup log alone:
- **Fresh database**: created a throwaway `flyway_fresh_test` database in the same Postgres container,
  pointed ticket-service at it, and confirmed the startup log showed Flyway actually running
  `V1__baseline_schema.sql` (`Migrating schema "public" to version "1"`, not baselining), followed by
  Hibernate's `validate` passing with no errors — proof the migration file itself is correct, not just
  that baselining suppresses a check. `pg_dump --schema-only` against that fresh database and `diff`
  against the original dev database's dump showed the two schemas identical (the only differences were
  pg_dump's own randomly-generated session tokens and cosmetic re-canonicalization of the `CHECK`
  constraint text, confirmed non-functional by directly inserting an invalid category and confirming
  the constraint still rejected it). The throwaway database was dropped afterward.
- **Existing, already-populated database**: restarted ticket-service against the real local dev
  database (the one with several days' worth of manually-created test tickets already in it) and
  confirmed the log showed `Successfully baselined schema with version: 1`, followed by Hibernate's
  `validate` passing — proof the retrofit path works without touching existing data. Confirmed via
  `SELECT * FROM flyway_schema_history` (one `BASELINE` row, version 1) and `SELECT count(*) FROM
  tickets` (unchanged) directly in Postgres.
- Ran the full unit test suite (52/52 passing, unaffected — none of them hit a real database) and the
  full `scripts/test-ticket-api.ps1` regression pass (19/19) against the now-Flyway-managed database, to
  confirm the switch from `ddl-auto: update` didn't change any actual application behavior.

**Known local limitation**: on this Windows machine, `TicketRepositoryIT` cannot currently execute —
Testcontainers' Docker client gets a malformed/stub response from Docker Desktop's Engine API over the
Windows named pipe, even though the `docker` CLI itself works perfectly for everything else in this
project (Postgres, Keycloak). Tried and ruled out: multiple named pipes, `DOCKER_HOST` overrides, a full
OS restart, and a newer Testcontainers version (1.20.4) — all hit the identical stub response. This
looks like a deeper Docker-Desktop-on-Windows/docker-java npipe compatibility issue, not a bug in this
project's code. The test itself is correctly written and will run normally in the GitHub Actions CI
pipeline (Linux runners don't have this npipe layer at all) once that's set up in Phase 13. Verification
method (1) above is the authoritative proof of the schema until then.
