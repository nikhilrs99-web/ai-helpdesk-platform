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
Java 21 &middot; Spring Boot 3 &middot; Spring Security &middot; Spring Data JPA &middot; Spring Cloud &middot; Spring AI &middot; PostgreSQL (+pgvector, full-text search) &middot; Redis &middot; Kafka &middot; Keycloak &middot; Docker &middot; Kubernetes &middot; Helm &middot; Argo CD &middot; Terraform &middot; AWS (EKS, RDS, S3, ElastiCache) &middot; Prometheus &middot; Grafana &middot; OpenTelemetry &middot; React &middot; TypeScript

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
