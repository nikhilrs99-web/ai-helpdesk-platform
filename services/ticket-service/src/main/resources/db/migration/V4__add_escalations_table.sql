CREATE TABLE escalations (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    ticket_id uuid NOT NULL,
    reason character varying(1000) NOT NULL,
    requested_by character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    decided_by character varying(255),
    CONSTRAINT escalations_pkey PRIMARY KEY (id),
    CONSTRAINT escalations_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[]))),
    CONSTRAINT fk_escalations_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id)
);

CREATE INDEX idx_escalations_ticket_id ON escalations (ticket_id);
