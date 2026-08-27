# Design Patterns Tracker

A living log of where and why each design pattern is used in this project. Entries are added the same day a
pattern actually ships in code — never written ahead of the implementation, so "Implemented" always means
implemented, not planned.

**Status legend**: `Implemented` | `Planned`

---

## State — Ticket lifecycle
**Status**: Implemented (Day 9)
**Where**: `ticket-service`, `Ticket` status transitions
**Why this over the obvious alternative**: A plain enum plus scattered `if/else` checks across the codebase makes
it easy to allow an illegal transition (e.g. `CLOSED` → `IN_PROGRESS`) from any call site that forgets to check.
The State pattern encodes "what transitions are legal from here" inside the state itself, so an illegal
transition becomes a structural impossibility rather than a bug someone has to remember to guard against.
**Implementation notes**: `TicketState` is a Java enum with a per-constant abstract method
(`legalNextStatuses()`), one constant per `TicketStatus` value — an idiomatic way to implement State in Java
without a separate class per state. `Ticket.changeStatus(TicketStatus)` is the *only* way to change a ticket's
status (the plain `setStatus` was removed); it delegates to `TicketState.transitionTo(...)`, which throws
`IllegalTicketTransitionException` for anything not legal. The actual graph is not purely linear — early closure
(`CLOSED`) is legal from any non-terminal state, `WAITING_FOR_CUSTOMER` can return to `IN_PROGRESS`, and
`RESOLVED` can be reopened back to `IN_PROGRESS` — a closer match to how support tickets really move than a
strict pipeline.
**Code**: [`domain/state/TicketState.java`](../../services/ticket-service/src/main/java/com/helpdesk/ticket/domain/state/TicketState.java),
[`domain/Ticket.java`](../../services/ticket-service/src/main/java/com/helpdesk/ticket/domain/Ticket.java)

## Strategy — Ticket routing rules
**Status**: Implemented (Day 11)
**Where**: `ticket-service`, deciding which team a new ticket is routed to
**Why this over the obvious alternative**: Routing rules change often (a new team is added, priority thresholds
get tweaked) and an `if/else` chain keyed on category grows unreadable fast. Strategy isolates the routing
decision behind one interface, so a whole new routing policy (e.g. an AI-driven one in Phase 5) can be swapped
in via dependency injection without `TicketController` changing at all.
**Implementation notes**: `RoutingStrategy` is a one-method interface (`determineTeam(Ticket)`); the only
implementation today, `CategoryBasedRoutingStrategy`, uses a `switch` expression over `TicketCategory` rather
than a `Map<TicketCategory, String>` — a switch with no default branch is exhaustive at compile time, so adding
a new `TicketCategory` value later fails the build until this is updated, instead of silently defaulting (or
NPEing) for a category nobody remembered to map. `TicketControllerRoutingTest` proves the interchangeability
claim directly: it injects a fake `RoutingStrategy` and asserts the controller uses it, with zero changes to
`TicketController` itself.
**Code**: [`routing/RoutingStrategy.java`](../../services/ticket-service/src/main/java/com/helpdesk/ticket/routing/RoutingStrategy.java),
[`routing/CategoryBasedRoutingStrategy.java`](../../services/ticket-service/src/main/java/com/helpdesk/ticket/routing/CategoryBasedRoutingStrategy.java)

## Factory — Ticket-type handlers
**Status**: Implemented (Day 12)
**Where**: `ticket-service`, constructing the right handler for BUG / BILLING / ACCESS / HOW_TO / FEATURE_REQUEST tickets
**Why this over the obvious alternative**: Each ticket type needs different required fields and different
downstream behavior (BUG captures browser/version, BILLING captures an invoice ID). A Factory centralizes
"given a category, build the right handler" so callers don't each need their own duplicated switch statement.
**Implementation notes**: `TicketTypeHandler` has one required-metadata concept per category; `BugTicketHandler`
and `BillingTicketHandler` each have real, distinct requirements, while ACCESS/HOW_TO/FEATURE_REQUEST
deliberately share one `DefaultTicketTypeHandler` rather than three near-empty classes — the Factory only needs
a distinct class where behavior actually differs. `TicketTypeHandlerFactory`'s constructor builds its lookup
map from every injected handler and **fails fast at application startup** (not at request time) if two handlers
claim the same category, or if any `TicketCategory` has no handler at all — the same "catch missing coverage as
early as possible" spirit as the Strategy pattern's exhaustive switch expression, just enforced at startup
instead of compile time, since this map is built dynamically from Spring-injected beans.
**Code**: [`tickettype/TicketTypeHandler.java`](../../services/ticket-service/src/main/java/com/helpdesk/ticket/tickettype/TicketTypeHandler.java),
[`tickettype/TicketTypeHandlerFactory.java`](../../services/ticket-service/src/main/java/com/helpdesk/ticket/tickettype/TicketTypeHandlerFactory.java)

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
