-- Requester email (JWT "email" claim at creation time) so notifications can reach the actual
-- requester instead of one shared mailbox. Nullable: older tickets and tokens without the claim.
ALTER TABLE tickets ADD COLUMN requester_email varchar(255);
