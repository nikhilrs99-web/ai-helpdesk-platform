# Agent Tool Contracts & Workflow

This document outlines the tools (functions) exposed to the LLM agent via Spring AI function calling, as well as the approval boundaries for read vs. write actions.

## Read-Only Tools (Autonomous Execution)
The agent is completely autonomous when utilizing the following tools to gather context:

1. **`getTicketStatus`**: Retrieves current state/lifecycle phase of a given ticket.
2. **`searchKnowledgeBase`**: Performs semantic RAG queries against pgvector to find solutions.
3. **`getSLAStatus`**: Determines if a ticket is breached or how much time remains on its SLA clock.
4. **`getCustomerTickets`**: Pulls historical context for the customer to prevent repetitive troubleshooting.

## Write Tools (Human-in-the-Loop)
The agent **cannot** unilaterally execute write actions that mutate state or notify users without human approval.

### `createEscalation`
- **Purpose**: Escalate a ticket to a specialized engineering team or management.
- **Workflow**:
  1. The AI decides an escalation is needed based on the user's chat input and SLA status.
  2. The AI invokes the `createEscalation` tool with `ticketId` and `reason`.
  3. The tool calls `POST /api/tickets/{ticketId}/escalations` on ticket-service (using the calling user's own forwarded token, not a separate service identity - see `AgentToolsConfig`'s class Javadoc), which persists a real `escalations` row with status `PENDING`. This has no effect on anything else yet.
  4. The AI informs the user: *"I have drafted an escalation request. A human agent will review and approve it shortly."*
  5. An authorized human agent calls `PATCH /api/tickets/{ticketId}/escalations/{id}/approve` (agent/admin only) to commit it, or `.../reject` to decline it.

  **Still pending**: there is no frontend UI for step 5 yet (only the API exists), and approval does not yet trigger any notification - both are natural follow-ups, not implemented in this pass.
