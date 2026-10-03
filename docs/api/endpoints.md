# API endpoints

Everything goes through the api-gateway (`http://localhost:8000` locally) and needs a Keycloak bearer
token (`Authorization: Bearer <jwt>`). Roles are `customer`, `agent`, `admin`. "Owner" means the
user who created the ticket. Errors use the shared `ApiError` JSON shape.

Gateway routes: `/api/tickets/**` and `/api/agents/**` -> ticket-service, `/api/articles/**` ->
kb-service, `/api/ai/**` -> ai-service, `/api/analytics/**` -> analytics-service,
`/api/notifications/**` -> notification-service.

## Tickets (ticket-service)
| Method & path | Who | Notes |
|---|---|---|
| `POST /api/tickets` | any user | Creates a ticket. Rate-limited per user (429). BUG needs `browser`+`appVersion` metadata, BILLING needs `invoiceId`. Stores the JWT `email` claim so notifications reach the requester. |
| `GET /api/tickets` | agent/admin: all; customer: own | Paged. Optional `requesterId` filter (agent/admin only). |
| `GET /api/tickets/{id}` | owner, agent, admin | |
| `PUT /api/tickets/{id}` | owner, agent, admin | Edit subject/description. |
| `PATCH /api/tickets/{id}/status` | agent, admin | Body `{"status": "..."}`. Illegal transitions return 409. Publishes `ticket.updated`. |
| `GET /api/tickets/{id}/sla-status` | owner, agent, admin | First-response SLA: target, deadline, breached, minutes remaining. |
| `POST /api/tickets/{id}/ai-resolve` | owner, agent, admin | Marks the ticket resolved by the AI. Only while OPEN/AI_TRIAGED and with no pending/approved escalation (else 409). Publishes `ticket.updated` with `aiResolved: true`. |

## Escalations (ticket-service) - human-in-the-loop
| Method & path | Who | Notes |
|---|---|---|
| `POST /api/tickets/{id}/escalations` | owner, agent, admin | Body `{"reason": "..."}`. Creates a `PENDING` escalation; it has no effect until approved. |
| `GET /api/tickets/{id}/escalations` | owner, agent, admin | |
| `PATCH /api/tickets/{id}/escalations/{escalationId}/approve` | agent, admin | Publishes `escalation.approved` (email to the requester). |
| `PATCH /api/tickets/{id}/escalations/{escalationId}/reject` | agent, admin | |

## Agent presence (ticket-service)
| Method & path | Who | Notes |
|---|---|---|
| `POST /api/agents/ping` | agent, admin | Marks the agent online for a TTL window (Redis). The UI pings every minute. |

## Knowledge base (kb-service)
| Method & path | Who | Notes |
|---|---|---|
| `GET /api/articles` , `GET /api/articles/{id}` | any user | |
| `GET /api/articles/search?q=...` | any user | Postgres full-text search, cached in Redis. |
| `POST /api/articles` , `PUT /api/articles/{id}` , `DELETE /api/articles/{id}` | agent, admin | |

## AI (ai-service)
| Method & path | Who | Notes |
|---|---|---|
| `POST /api/ai/rag/search` | any user | Body `{"query": "..."}`. Hybrid retrieval (vector + full-text, RRF) then an LLM answer. |
| `POST /api/ai/ticket/analyze` | any user | Body `{"description": "..."}`. Returns validated `{"sentiment","category"}`. |
| `POST /api/ai/ingest` | agent, admin | Body `{"id","title","content"}`. Chunks the article and replaces its previous embeddings. |
| `POST /api/ai/agent/chat` | any user | Body `{"message": "..."}`. Tool-calling agent acting with the caller's own token (ticket status, SLA, history, KB search, escalate, resolve). Needs a live OpenAI key. |

LLM calls sit behind a circuit breaker: when it is open the API answers 503.

## Analytics (analytics-service) - agent/admin only
| Method & path | Notes |
|---|---|
| `GET /api/analytics/dashboard` | `totalTickets`, `slaCompliancePercentage`, `aiResolutionPercentage`. |
| `GET /api/analytics/volume?days=7` | Tickets created per UTC day, oldest first, zero-filled, `days` clamped to 1-90. |

## Events (Kafka topic `ticket-events`)
See [`docs/kafka/event-schema.md`](../kafka/event-schema.md).
