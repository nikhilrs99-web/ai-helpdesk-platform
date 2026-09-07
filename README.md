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
| Phase | Description |
|---|---|
| Phase 1: Setup | Scaffolded `ticket-service` (Spring Boot 3.2, Java 21, JPA, PostgreSQL, REST controllers). Designed domain model with Strategy/State patterns for ticket routing and lifecycle management. Integrated Keycloak for stateless JWT authentication and role-based access control (Admin, Agent, Customer). Wrote 52 comprehensive unit and integration tests. |
| Phase 2: Core Microservices | Scaffolded `kb-service` with PostgreSQL full-text search (`tsvector`), and a basic `notification-service`. Introduced Flyway for database migrations. Created `api-gateway` (Spring Cloud Gateway) for centralized routing and JWT validation. Containerized all services using multi-stage Dockerfiles and wired them into `docker-compose.yml`. |
| Phase 3: Event-Driven Architecture | Replaced direct REST calls with a Transactional Outbox pattern writing safely to PostgreSQL, polled via OutboxWorker to publish idempotently to a KRaft Kafka cluster. `notification-service` and `analytics-service` now consume these events robustly with DLQ and Retry configurations. Scheduled an SLA-breach cron job. |
| Phase 4: Caching & Rate Limiting | Spun up Redis within docker-compose. Applied `@Cacheable` annotations to `kb-service` for cache-aside reads. Built a custom RateLimiterService in `ticket-service` to cap ticket creations per user, and an AgentPresenceService tracking online staff via TTL expirations. |
| Phase 5: Hybrid RAG Pipeline | Built `ai-service` leveraging Spring AI. Initialized `pgvector` vector store with HNSW indexing. Implemented `/ingest` for generating Knowledge Base embeddings and `/rag/search` for semantic search and Retrieval-Augmented Generation. Added `/ticket/analyze` for auto-categorization and sentiment analysis. |
| Phase 6: AI Evaluation | Built a golden Q&A dataset. Developed a custom evaluation harness (`RagEvaluationTest`) to measure Precision@K and LLM-as-a-judge for answer faithfulness. Tuned search parameters (TopK=5, similarityThreshold=0.75) based on eval feedback to reduce hallucinations. |
| Phase 7: AI Agent Tool-Calling | Configured Spring AI function calling. Defined read-only tools: `getTicketStatus`, `searchKnowledgeBase`, `getSLAStatus`, `getCustomerTickets`. Implemented a human-in-the-loop write tool: `createEscalation`. Documented the Agentic workflow. |
| Phase 8: Analytics & Frontend Kickoff | Created JPA schema and Kafka consumer to persist ticket metrics in `analytics-service`. Built REST endpoints for dashboard SLAs and AI resolution rates. Scaffolded the React+Vite+Tailwind frontend SPA and wired the base routing. |
| Phase 9: Frontend Polish & Dockerization | Added a responsive Recharts-powered analytics dashboard. Built an AgentPresence component. Implemented polished Tailwind layout and responsive states. Added a multi-stage Nginx Dockerfile for the frontend and wired it into docker-compose. |
| Phase 10: Kubernetes Architecture | Created an Umbrella Helm chart wrapping the entire platform architecture. Implemented Deployments, Services, ConfigMaps, and Secrets. Added Bitnami subcharts for Postgres, Kafka, Redis, and Keycloak. Added Horizontal Pod Autoscaling (HPA) and configured liveness/readiness probes. |
| Phase 11: Observability | Added `opentelemetry-spring-boot-starter` (OTel) to all Java microservices for automatic distributed tracing. Integrated Grafana, Prometheus, and Tempo into docker-compose for metrics and trace storage via OTLP. Configured Prometheus scraping rules and documented the Trace-to-Log correlation architecture. |
| Phase 12: Terraform & AWS | Wrote Infrastructure-as-Code modules for a production AWS deployment. Provisioned a 3-AZ VPC, an EKS cluster, managed RDS PostgreSQL (pgvector-enabled), ElastiCache Redis, and ECR repositories. Configured remote S3/DynamoDB state backend and documented the deployment/teardown process. |
