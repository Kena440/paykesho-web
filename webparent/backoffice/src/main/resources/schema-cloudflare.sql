-- PayKesho Lenco settlement schema additions.
-- Idempotent so the Cloudflare back-office container can safely run this
-- before Hibernate validates the production schema.

ALTER TABLE app.transactions
    ADD COLUMN IF NOT EXISTS settlement_status VARCHAR(32),
    ADD COLUMN IF NOT EXISTS settlement_reference VARCHAR(128),
    ADD COLUMN IF NOT EXISTS settlement_reason VARCHAR(255),
    ADD COLUMN IF NOT EXISTS settlement_updated_at TIMESTAMP;

ALTER TABLE app.merchants
    ADD COLUMN IF NOT EXISTS lenco_transfer_recipient_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS payout_method VARCHAR(32),
    ADD COLUMN IF NOT EXISTS payout_operator VARCHAR(16),
    ADD COLUMN IF NOT EXISTS payout_phone VARCHAR(20),
    ADD COLUMN IF NOT EXISTS payout_bank_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS payout_account_number VARCHAR(64);
