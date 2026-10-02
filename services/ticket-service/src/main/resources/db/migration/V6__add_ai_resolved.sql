-- Set when the AI agent resolved the ticket on its own (no escalation, no human agent) -
-- feeds analytics' AI resolution rate via TicketUpdatedEvent.aiResolved.
ALTER TABLE tickets ADD COLUMN ai_resolved boolean NOT NULL DEFAULT false;
