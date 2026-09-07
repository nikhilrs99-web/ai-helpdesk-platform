# ADR 0004: Local dev security posture vs. production hardening requirements

**Status**: Accepted

## Context
`docker-compose.yml` runs the whole platform locally with most infrastructure components in their least-friction, least-secure mode: Keycloak in `start-dev`, Kafka with only `PLAINTEXT` listeners, Redis with no `requirepass`, and Grafana with anonymous access enabled as Admin (`GF_AUTH_ANONYMOUS_ENABLED=true`, `GF_AUTH_ANONYMOUS_ORG_ROLE=Admin`). Postgres and Keycloak do have real credentials, but they're sourced from a plaintext `.env` file with no rotation or vaulting.

This is intentional for a local, single-developer environment — it removes friction (no cert setup, no SASL handshakes, no login prompts while iterating) — but it means the stack as committed is not something that should ever be pointed at real user data or exposed beyond localhost.

## Decision
Accept the current insecure-by-default posture for local development, and treat closing each gap below as a distinct, separately-scoped piece of work rather than something to retrofit piecemeal. Each row is a real ADR/PR of its own when tackled, not a config toggle to flip in isolation.

| Gap | Current state | What production requires |
|---|---|---|
| Secrets | Plaintext `.env`, committed `.env.example` as the only template | Vault / AWS Secrets Manager / Kubernetes Sealed Secrets, with rotation |
| Kafka | `PLAINTEXT` listeners, no ACLs | `SASL_SSL` + per-topic ACLs scoped to each service's principal |
| Redis | No `requirepass`, no TLS | `requirepass` + TLS, or a managed Redis (ElastiCache) with an IAM-backed auth token |
| Grafana | Anonymous Admin access | Real auth provider (OAuth/OIDC via Keycloak), anonymous access disabled |
| Postgres | Password auth only, no TLS | TLS-required connections, least-privilege roles per service (not one shared `helpdesk_app` superuser-ish role) |
| Keycloak | `start-dev` mode | Production mode with a real database backend, HTTPS, and hardened realm settings |
| Service-to-service | Plaintext HTTP inside the Docker network | mTLS via a service mesh (or at minimum TLS termination at the gateway with internal network policies) |
| Network | Every container reachable from every other container | Kubernetes `NetworkPolicy` restricting which services can reach which |

## Consequences
- The stack is safe to run and demo locally, but **must not** be deployed as-is to any environment reachable outside localhost.
- The existing Helm charts (Phase 10) and OTel/Grafana observability (Phase 11) give a real base to layer hardening onto — this ADR documents the gap, it doesn't block using the platform for its intended purpose (a portfolio/demo of the application-layer architecture: ticket lifecycle, outbox, RAG, agent tool-calling).
- Anyone extending this project toward a real deployment should treat this table as a checklist, not optional polish — Kafka ACLs and Grafana auth in particular are a few hours of work each, not a redesign.
