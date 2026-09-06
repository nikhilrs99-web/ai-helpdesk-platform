CREATE TABLE ticket_metrics (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL,
    category VARCHAR(50),
    status VARCHAR(50),
    sla_breached BOOLEAN DEFAULT FALSE,
    ai_auto_resolved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE
);
