# AI-Powered Helpdesk Platform

A RAG-based support desk built as event-driven microservices — a portfolio project demonstrating Spring Boot, Kafka, Redis, Spring AI, Kubernetes, and AWS end to end. AI is a feature inside a strong backend system, not the whole project: ticket classification, retrieval-augmented draft replies, and an agentic tool-calling assistant sit on top of a properly modeled ticket lifecycle, transactional-outbox event publishing, and full observability.

## Status
In active development — built incrementally, one day at a time. See the Build Log below.

## Repository layout
```
common/            shared DTOs, enums, and versioned Kafka event records used by every service
services/         one folder per microservice (ticket, kb, ai, notification, analytics)
infrastructure/    docker, kubernetes, helm, terraform
docs/
  architecture/    diagrams
  api/             endpoint documentation
  kafka/           event schemas and topic design
  rag/             retrieval pipeline and evaluation notes
  database/        schema and entity design
  decisions/       Architecture Decision Records (ADRs) — the "why" behind each major choice
```

## Running locally
```
cp .env.example .env      # fill in local values (never commit .env)
docker compose up -d      # starts PostgreSQL (pgvector) and Keycloak
```
See [docs/architecture/keycloak-setup.md](docs/architecture/keycloak-setup.md) for the realm/roles/test-user setup and how to get a local test token.

## Architecture
Diagram added once the core services are online.

## Tech Stack
Java 21 &middot; Spring Boot 3 &middot; Spring Security &middot; Spring Data JPA &middot; Flyway &middot; Spring Cloud &middot; Spring AI &middot; PostgreSQL (+pgvector, full-text search) &middot; Redis &middot; Kafka &middot; Keycloak &middot; Docker &middot; Kubernetes &middot; Helm &middot; Argo CD &middot; Terraform &middot; AWS (EKS, RDS, S3, ElastiCache) &middot; Prometheus &middot; Grafana &middot; OpenTelemetry &middot; React &middot; TypeScript

See [docs/decisions](docs/decisions) for the reasoning behind key choices, including why PostgreSQL replaces a separate MongoDB store.

## Design Patterns
See [docs/architecture/design-patterns.md](docs/architecture/design-patterns.md) for where each design pattern (State, Strategy, Factory, Observer, Circuit Breaker) is used and why it was chosen over the obvious alternative. Tracked as each one actually ships, not planned ahead of the code.

## Build Log
| Day | What shipped |
|---|---|
| Day 1 | Project scaffold: parent Maven POM, .gitignore, README |
| Day 2 | Repository restructured into services/, infrastructure/, docs/; first three ADRs added |
| Day 3 | Five empty Spring Boot modules (ticket, kb, ai, notification, analytics) with health endpoints, wired into the parent POM |
| Day 4 | docker-compose.yml with PostgreSQL (pgvector 0.8.6), init script enabling the extension, .env.example added |
| Day 5 | common module added: shared enums, a DomainEvent contract, three versioned Kafka event records, and a shared ApiError DTO, wired into every service; design-patterns tracker started |
| Day 6 | Keycloak added via docker-compose with a realm-as-code import (realm, client, roles, test users); Postgres host port fixed to 5433 to resolve a conflict with an unrelated local container; verified real token issuance end to end |
| Day 7 | ticket-service configured as an OAuth2 resource server validating Keycloak JWTs, with a custom converter reading Keycloak's realm_access.roles claim; verified end to end: no token -> 401, valid token -> 200 with the correct role |
| Day 8 | Four JPA entities (Ticket, TicketComment, Agent, Sla) with auditing, connected to Postgres; schema verified directly in the database; a Testcontainers integration test added (correct, but currently blocked locally by a Windows Docker Desktop npipe issue - see docs/database/schema-notes.md) |
| Day 9 | Ticket lifecycle State pattern implemented (TicketState, Ticket.changeStatus); setStatus removed so illegal transitions are structurally impossible; 15 unit tests, all passing; design-patterns tracker updated |
| Day 10 | First REST API: create/get/list/update/change-status endpoints for tickets, with validation and a global exception handler using the ApiError DTO; verified end to end (401/201/400/200/404/409); fixed a real -parameters compiler gap; documented in docs/api/tickets.md, including a known (deliberately deferred) authorization gap |
| Day 11 | Strategy pattern for ticket routing: RoutingStrategy interface + CategoryBasedRoutingStrategy (exhaustive switch expression), wired into ticket creation via a new routedTeam field; interchangeability proven with a fake-strategy test; verified end to end across all 5 categories, both via the API and directly in Postgres; design-patterns tracker updated |
| Day 12 | Factory pattern for ticket-type handlers: BugTicketHandler/BillingTicketHandler with real per-category required-metadata validation, DefaultTicketTypeHandler shared by the 3 categories with no special requirements, TicketTypeHandlerFactory that fails fast at startup on duplicate/missing category coverage; new Ticket.metadata (JPA @ElementCollection, a new ticket_metadata table); 14 new tests (35 total); verified end to end (400s for missing keys, 201s with defaults applied) both via the API and in Postgres |
| Day 13 | Role-based method security closing the tracked IDOR gap: @PreAuthorize + a custom TicketSecurity("ticketSecurity") bean gate GET/PUT by id to owner-or-agent/admin, PATCH status to agent/admin only; GET (list) is scoped (not gated) by role via a different repository query per caller; AccessDeniedException now maps to a 403 ApiError; also fixed a real pre-existing bug this surfaced - Ticket.metadata's default LAZY fetch threw LazyInitializationException on every GET/PUT/PATCH once open-in-view:false met a real loaded ticket, fixed via FetchType.EAGER; 9 new tests (44 total); verified end to end over real HTTP with real Keycloak tokens (test-customer, test-agent, and a new test-customer-2 fixture for cross-owner denial) and confirmed in Postgres |
| Day 14 | Closed a real automated-test gap rather than add tests for their own sake: GlobalExceptionHandler's 400 (validation failure, both simple field violations and Day 12's category-specific missing-metadata message), 404 (ticket not found), and 409 (illegal state transition) HTTP-status mappings had all been verified manually via curl/PowerShell on earlier days but never pinned down as an automated test - a refactor could have silently broken any of them with nothing failing; reviewed the existing State/Strategy/Factory suites first and found them already thorough, so no changes needed there. 8 new MockMvc-based tests, all exercising real validation/state-machine/factory logic rather than mocks standing in for it (52 total, all passing) |
| Day 15 | scripts/test-ticket-api.ps1 and the Postman collection were both stale against Days 11-13 (routedTeam, category metadata requirements, and Day 13's authorization rules weren't reflected - the old script even asserted a customer could change a ticket's status, which now correctly 403s); rewrote both to cover the full current contract with three real Keycloak identities (test-customer, test-agent, test-customer-2): routing/metadata on create, owner-vs-agent GET/PUT, agent-only status changes, list scoping (with the customer token's own "sub" claim decoded locally and checked against every returned ticket), and the 400/404/409/403 error paths: 19 checks in the PowerShell script, all passing end to end against the live service and cross-checked in Postgres |
| Day 16 | Replaced spring.jpa.hibernate.ddl-auto: update with Flyway now that the domain model has held stable since Day 13: V1__baseline_schema.sql captures the exact schema pg_dump reported (not a hand-reconstruction from the JPA annotations), ddl-auto is now validate (Hibernate checks its mappings match reality instead of altering it), and spring.flyway.baseline-on-migrate handles this app's specific retrofit problem - tables that already existed before Flyway was introduced. Verified both paths for real: a fresh throwaway database actually ran V1 end to end (diffed identical to the original schema, with the one cosmetic CHECK-constraint text difference confirmed non-functional by testing an invalid insert), and the real, already-populated local dev database baselined cleanly with zero data loss. Full regression re-run on top of the Flyway-managed schema: 52/52 unit tests, 19/19 live API checks |
| Day 17 | kb-service scaffolding: KnowledgeArticle entity with PostgreSQL `tsvector` for full-text search (populated automatically via database trigger), Flyway schema migration, JPA Auditing, and CRUD REST endpoints including a keyword search endpoint utilizing `ts_rank` sorting |
| Day 18 | 
otification-service skeleton added: Implemented Observer-style notifier pattern (NotificationObserver, NotificationDispatcher) and a temporary direct REST endpoint (NotificationController) to act as the notification receiver before Kafka is introduced |
| Phase 2 Wrap-up | Completed Days 19-25: Wired ticket-service to notification-service via direct REST (Day 19), added Testcontainers integration test for kb-service (Day 20), scaffolded api-gateway with routing (Day 21) and centralized JWT validation (Day 22), added multi-stage Dockerfiles for all microservices (Days 23-24), and wired everything into docker-compose.yml for a full stack deployment (Day 25) |

