# Design Patterns Tracker

A living log of where and why each design pattern is used in this project. Entries are added the same day a
pattern actually ships in code — never written ahead of the implementation, so "Implemented" always means
implemented, not planned.

**Status legend**: `Implemented` | `Planned`

---

## State — Ticket lifecycle
**Status**: Planned (Week 2)
**Where**: `ticket-service`, `Ticket` status transitions (`OPEN` → `AI_TRIAGED` → `ASSIGNED` → `IN_PROGRESS` →
`WAITING_FOR_CUSTOMER` → `RESOLVED` → `CLOSED`)
**Why this over the obvious alternative**: A plain enum plus scattered `if/else` checks across the codebase makes
it easy to allow an illegal transition (e.g. `CLOSED` → `IN_PROGRESS`) from any call site that forgets to check.
The State pattern encodes "what transitions are legal from here" inside the state itself, so an illegal
transition becomes a structural impossibility rather than a bug someone has to remember to guard against.
**Code**: _link added once implemented_

## Strategy — Ticket routing rules
**Status**: Planned (Week 3)
**Where**: `ticket-service`, deciding which team/agent a new ticket is routed to
**Why this over the obvious alternative**: Routing rules change often (a new team is added, priority thresholds
get tweaked) and an `if/else` chain keyed on category grows unreadable fast. Strategy isolates each routing rule
as its own class, so adding a rule means adding a class, not editing a growing conditional everyone is afraid to touch.
**Code**: _link added once implemented_

## Factory — Ticket-type handlers
**Status**: Planned (Week 3)
**Where**: `ticket-service`, constructing the right handler for BUG / BILLING / ACCESS / HOW_TO / FEATURE_REQUEST tickets
**Why this over the obvious alternative**: Each ticket type needs different required fields and different
downstream behavior (BUG captures browser/version, BILLING captures an invoice ID). A Factory centralizes
"given a category, build the right handler" so callers don't each need their own duplicated switch statement.
**Code**: _link added once implemented_

## Observer — Notification dispatch
**Status**: Planned (Week 4)
**Where**: `notification-service`, notifying interested parties when a ticket event happens
**Why this over the obvious alternative**: Multiple things may want to react to the same ticket event (email,
in-app toast, Slack later) without `ticket-service` needing to know about every one of them individually.
Observer decouples "an event happened" from "here is everyone who cares."
**Code**: _link added once implemented_

## Circuit Breaker (resilience pattern) — AI service calls
**Status**: Planned (Week 10)
**Where**: `ai-service`, calling the external LLM API
**Why this over the obvious alternative**: Without it, an LLM outage means every ticket-processing request hangs
on a timeout one at a time, potentially exhausting thread pools. A circuit breaker fails fast after repeated
failures and falls back to a safe default (skip AI drafting, route to a human) instead of the outage cascading
into the rest of the system.
**Code**: _link added once implemented_
