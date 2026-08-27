# Ticket API (ticket-service)

All endpoints require a valid Keycloak access token (`Authorization: Bearer <token>`, see
[docs/architecture/keycloak-setup.md](../architecture/keycloak-setup.md)). Beyond that, each endpoint
enforces one of two rules based on the caller's realm role (`customer`/`agent`/`admin`) — see
**Authorization rules** below.

## `POST /api/tickets`
Creates a ticket. `requesterId` is never taken from the request body — it's always the authenticated
token's `sub` claim, so a client cannot create a ticket on someone else's behalf.

Request:
```json
{ "subject": "Double charged", "description": "Billed twice this month", "category": "BILLING" }
```
`subject` (required, max 200 chars), `description` (required, max 5000 chars), `category` (required,
one of `BUG`/`BILLING`/`ACCESS`/`HOW_TO`/`FEATURE_REQUEST`), `metadata` (optional map of strings,
requirements depend on `category` — see below).

On creation, `routedTeam` is also set automatically via the Strategy pattern (see
[docs/architecture/design-patterns.md](../architecture/design-patterns.md)) — not client-supplied,
purely derived from `category`.

### Category-specific metadata (Factory pattern)
Which `metadata` keys are required depends on `category`, enforced by the matching `TicketTypeHandler`:

| Category | Required keys | Notes |
|---|---|---|
| `BUG` | `browser`, `appVersion` | both required, or `400` |
| `BILLING` | `invoiceId` | required; `currency` defaults to `"USD"` if not supplied |
| `ACCESS`, `HOW_TO`, `FEATURE_REQUEST` | none | `metadata` can be omitted entirely |

A missing required key returns `400` with an `ApiError` naming exactly which keys are missing, e.g.
`"Missing required metadata for category BUG: [appVersion, browser]"`.

Response: `201 Created`, `Location: /api/tickets/{id}`, body is a `TicketResponse` (see below).

## `GET /api/tickets/{id}`
Returns a `TicketResponse`. Owner or `agent`/`admin` only (see **Authorization rules**) — `403` for
anyone else, `404` with an `ApiError` body if the id doesn't exist at all.

## `GET /api/tickets?page=0&size=20`
Paginated list (standard Spring Data `Pageable` query params: `page`, `size`, `sort`). Returns a `Page`
wrapper with `content`, `totalElements`, `totalPages`, etc. The *content* is scoped by role, not gated:
a `customer` gets only their own tickets, an `agent`/`admin` gets every ticket — see **Authorization
rules**.

## `PUT /api/tickets/{id}`
Updates `subject`/`description` only. Owner or `agent`/`admin` only. `404` if not found, `400` if
validation fails, `403` if the caller isn't the owner and isn't `agent`/`admin`.

## `PATCH /api/tickets/{id}/status`
Changes the ticket's status via the State pattern (see
[docs/architecture/design-patterns.md](../architecture/design-patterns.md)). `agent`/`admin` only —
customers cannot drive a ticket's workflow state, even on their own ticket.
```json
{ "status": "AI_TRIAGED" }
```
`200` with the updated `TicketResponse` if the transition is legal from the ticket's current status,
`409 Conflict` with an `ApiError` if it isn't, `404` if the ticket doesn't exist, `403` if the caller
isn't `agent`/`admin`.

## `TicketResponse` shape
```json
{
  "id": "uuid",
  "subject": "string",
  "description": "string",
  "category": "BUG|BILLING|ACCESS|HOW_TO|FEATURE_REQUEST",
  "status": "OPEN|AI_TRIAGED|ASSIGNED|IN_PROGRESS|WAITING_FOR_CUSTOMER|RESOLVED|CLOSED",
  "requesterId": "keycloak subject uuid",
  "assignedAgentId": "uuid or null",
  "routedTeam": "engineering|billing|support|product",
  "metadata": { "key": "value" },
  "createdAt": "instant",
  "updatedAt": "instant"
}
```
Deliberately a separate shape from the `Ticket` JPA entity — see the "Why" note in the Day 10 build log.

## Errors
Every error response uses `common`'s `ApiError` shape:
```json
{ "timestamp": "...", "status": 404, "error": "Not Found", "message": "...", "path": "/api/tickets/..." }
```

## Authorization rules (Day 13)
Every endpoint requires a valid token; beyond that, two distinct rules apply depending on what the
endpoint does:

| Endpoint | Rule |
|---|---|
| `POST /api/tickets` | any authenticated caller — `requesterId` is always the token's own `sub`, so there's no cross-user write to guard against |
| `GET /api/tickets/{id}` | owner or `agent`/`admin` |
| `GET /api/tickets` | not gated — *scoped*: `customer` sees only their own tickets, `agent`/`admin` sees all |
| `PUT /api/tickets/{id}` | owner or `agent`/`admin` |
| `PATCH /api/tickets/{id}/status` | `agent`/`admin` only |

**Owner-or-elevated checks** (`GET`/`PUT` by id) are enforced via `@PreAuthorize` calling a custom
`TicketSecurity.isOwner(authentication, ticketId)` bean, not `@PostAuthorize` — ownership can't be read
off the request (ticket ids are opaque UUIDs), and `PUT` mutates, so the check has to run *before* the
method body, or a denied caller's write would already have been persisted by the time the 403 came back.

**List scoping** is not a `@PreAuthorize` concern at all: a `Page<T>` can't be filtered row-by-row by a
method-level annotation, so `GET /api/tickets` runs a different repository query depending on the
caller's role (`findByRequesterId` vs `findAll`) rather than gating a single query's result.

**A denied owner check returns `403`, not `404`, even when the ticket doesn't exist at all** — a
`customer` requesting an id that isn't theirs gets the same `403` whether that ticket belongs to someone
else or doesn't exist, since ownership can't be proven either way. This is a deliberate choice, not an
oversight: ticket ids are random UUIDs (not sequentially enumerable), so masking existence behind a
`404` buys little here, and a uniform `403` is simpler to reason about than trying to distinguish the two
cases.
