# AI-Powered Helpdesk Platform

A RAG-based support desk built as event-driven microservices — a portfolio project demonstrating Spring Boot, Kafka, Redis, Spring AI, Kubernetes, and AWS end to end. AI is a feature inside a strong backend system, not the whole project: ticket classification, retrieval-augmented draft replies, and an agentic tool-calling assistant sit on top of a properly modeled ticket lifecycle, transactional-outbox event publishing, and full observability.

## Status
In active development — built incrementally, one day at a time. See the Build Log below.

## Repository layout
```
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
docker compose up -d      # starts PostgreSQL with pgvector enabled
```

## Architecture
Diagram added once the core services are online.

## Tech Stack
Java 21 &middot; Spring Boot 3 &middot; Spring Security &middot; Spring Data JPA &middot; Spring Cloud &middot; Spring AI &middot; PostgreSQL (+pgvector, full-text search) &middot; Redis &middot; Kafka &middot; Keycloak &middot; Docker &middot; Kubernetes &middot; Helm &middot; Argo CD &middot; Terraform &middot; AWS (EKS, RDS, S3, ElastiCache) &middot; Prometheus &middot; Grafana &middot; OpenTelemetry &middot; React &middot; TypeScript

See [docs/decisions](docs/decisions) for the reasoning behind key choices, including why PostgreSQL replaces a separate MongoDB store.

## Build Log
| Day | What shipped |
|---|---|
| Day 1 | Project scaffold: parent Maven POM, .gitignore, README |
| Day 2 | Repository restructured into services/, infrastructure/, docs/; first three ADRs added |
| Day 3 | Five empty Spring Boot modules (ticket, kb, ai, notification, analytics) with health endpoints, wired into the parent POM |
| Day 4 | docker-compose.yml with PostgreSQL (pgvector 0.8.6), init script enabling the extension, .env.example added |
