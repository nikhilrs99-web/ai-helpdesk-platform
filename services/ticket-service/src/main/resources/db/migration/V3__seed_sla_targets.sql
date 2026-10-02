-- sla_targets has existed since V1 but was never seeded, so there has never been a real
-- target for anything to check breach against. FIRST_RESPONSE minutes per category; BILLING's
-- 240 matches the example already used in docs/database/schema-notes.md.
INSERT INTO sla_targets (id, created_at, updated_at, category, sla_type, target_minutes) VALUES
    (gen_random_uuid(), now(), now(), 'ACCESS', 'FIRST_RESPONSE', 60),
    (gen_random_uuid(), now(), now(), 'BUG', 'FIRST_RESPONSE', 120),
    (gen_random_uuid(), now(), now(), 'BILLING', 'FIRST_RESPONSE', 240),
    (gen_random_uuid(), now(), now(), 'HOW_TO', 'FIRST_RESPONSE', 480),
    (gen_random_uuid(), now(), now(), 'FEATURE_REQUEST', 'FIRST_RESPONSE', 1440);
