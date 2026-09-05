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
  3. The tool executes on the backend, generating a draft escalation object mapped with status `PENDING_HUMAN_APPROVAL`.
  4. The AI informs the user: *"I have drafted an escalation request. A human agent will review and approve it shortly."*
  5. An authorized human agent reviews the draft via the frontend UI and clicks "Approve". Only then does the backend permanently commit the escalation and trigger notifications.
