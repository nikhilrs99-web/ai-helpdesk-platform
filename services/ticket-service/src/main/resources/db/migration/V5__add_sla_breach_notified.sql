-- Numbered V5, not V3: fix/ai-service-agent-tools (separate, not-yet-merged PR) already
-- claims V3 (seed sla_targets) and V4 (escalations table). Merge that PR before this one,
-- or before deploying this migration - Flyway rejects out-of-order versions by default.
ALTER TABLE tickets ADD COLUMN sla_breach_notified boolean NOT NULL DEFAULT false;
