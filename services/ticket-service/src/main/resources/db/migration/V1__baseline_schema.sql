-- Baseline migration (Day 16): captures the exact schema that
-- spring.jpa.hibernate.ddl-auto=update had already produced across Days 8, 9, 11, 12, and 13
-- (Ticket/TicketComment/Agent/Sla entities, the ticket_metadata @ElementCollection table, and
-- every column/constraint Hibernate derived along the way), pulled directly from the real
-- database with pg_dump rather than reconstructed by hand from the JPA annotations - this is
-- meant to be a byte-for-byte snapshot of what's actually there, not a rewrite.
--
-- Existing environments (this local dev database) are baselined onto this version via
-- spring.flyway.baseline-on-migrate (see application.yml) rather than having this script
-- re-run against tables that already exist. A genuinely fresh database (a new environment, or
-- CI once it exists) runs this script for real and ends up with the identical schema - see
-- docs/database/schema-notes.md for how that was verified.

CREATE TABLE agents (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    display_name character varying(255) NOT NULL,
    keycloak_subject_id character varying(255) NOT NULL,
    team character varying(255)
);

CREATE TABLE tickets (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    category character varying(255) NOT NULL,
    description text NOT NULL,
    requester_id character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    subject character varying(255) NOT NULL,
    assigned_agent_id uuid,
    routed_team character varying(255),
    CONSTRAINT tickets_category_check CHECK (((category)::text = ANY ((ARRAY['BUG'::character varying, 'BILLING'::character varying, 'ACCESS'::character varying, 'HOW_TO'::character varying, 'FEATURE_REQUEST'::character varying])::text[]))),
    CONSTRAINT tickets_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'AI_TRIAGED'::character varying, 'ASSIGNED'::character varying, 'IN_PROGRESS'::character varying, 'WAITING_FOR_CUSTOMER'::character varying, 'RESOLVED'::character varying, 'CLOSED'::character varying])::text[])))
);

CREATE TABLE ticket_comments (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    ai_drafted boolean NOT NULL,
    author_id character varying(255) NOT NULL,
    body text NOT NULL,
    ticket_id uuid NOT NULL
);

CREATE TABLE ticket_metadata (
    ticket_id uuid NOT NULL,
    meta_value character varying(255),
    meta_key character varying(255) NOT NULL
);

CREATE TABLE sla_targets (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    category character varying(255) NOT NULL,
    sla_type character varying(255) NOT NULL,
    target_minutes integer NOT NULL,
    CONSTRAINT sla_targets_category_check CHECK (((category)::text = ANY ((ARRAY['BUG'::character varying, 'BILLING'::character varying, 'ACCESS'::character varying, 'HOW_TO'::character varying, 'FEATURE_REQUEST'::character varying])::text[])))
);

ALTER TABLE ONLY agents
    ADD CONSTRAINT agents_pkey PRIMARY KEY (id);

ALTER TABLE ONLY tickets
    ADD CONSTRAINT tickets_pkey PRIMARY KEY (id);

ALTER TABLE ONLY ticket_comments
    ADD CONSTRAINT ticket_comments_pkey PRIMARY KEY (id);

ALTER TABLE ONLY ticket_metadata
    ADD CONSTRAINT ticket_metadata_pkey PRIMARY KEY (ticket_id, meta_key);

ALTER TABLE ONLY sla_targets
    ADD CONSTRAINT sla_targets_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sla_targets
    ADD CONSTRAINT uknimyb6mei6kvk80t9wmrh0ekv UNIQUE (category, sla_type);

ALTER TABLE ONLY agents
    ADD CONSTRAINT ukq8wvpsyis9c2p2e8jh1wd4noh UNIQUE (keycloak_subject_id);

ALTER TABLE ONLY tickets
    ADD CONSTRAINT fkcb1to4sfelrsaqjnau3k0q155 FOREIGN KEY (assigned_agent_id) REFERENCES agents(id);

ALTER TABLE ONLY ticket_comments
    ADD CONSTRAINT fkdoce3fj1osdn71h25dhfs160v FOREIGN KEY (ticket_id) REFERENCES tickets(id);

ALTER TABLE ONLY ticket_metadata
    ADD CONSTRAINT fkgm9ameu6rnce67yqye7ogwtlk FOREIGN KEY (ticket_id) REFERENCES tickets(id);
