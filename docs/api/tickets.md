# Ticket API (ticket-service)

All endpoints require a valid Keycloak access token (`Authorization: Bearer <token>`, see
[docs/architecture/keycloak-setup.md](../architecture/keycloak-setup.md)). None require a specific role
yet — see **Known limitation** below.

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
Returns a `TicketResponse`, or `404` with an `ApiError` body if the id doesn't exist.

## `GET /api/tickets?page=0&size=20`
Paginated list (standard Spring Data `Pageable` query params: `page`, `size`, `sort`). Returns a `Page`
wrapper with `content`, `totalElements`, `totalPages`, etc.

## `PUT /api/tickets/{id}`
Updates `subject`/`description` only. `404` if not found, `400` if validation fails.

## `PATCH /api/tickets/{id}/status`
Changes the ticket's status via the State pattern (see
[docs/architecture/design-patterns.md](../architecture/design-patterns.md)).
```json
{ "status": "AI_TRIAGED" }
```
`200` with the updated `TicketResponse` if the transition is legal from the ticket's current status,
`409 Conflict` with an `ApiError` if it isn't, `404` if the ticket doesn't exist.

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

## Known limitation (deliberate, not yet closed)
**No ownership or role checks yet.** Any authenticated user — regardless of role, regardless of whether
they created the ticket — can currently read, update, or change the status of *any* ticket. This is an
IDOR-shaped gap (OWASP-relevant) and is deliberately deferred to Day 13, when Keycloak realm roles are
wired into method-level `@PreAuthorize` checks. Flagged here so it's a known, tracked gap rather than a
silent one.
