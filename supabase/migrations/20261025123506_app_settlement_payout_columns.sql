-- Adds the Lenco settlement and merchant payout columns that the shared
-- entities (commons Transaction and Merchant) already map, to the "app"
-- schema every portal uses. Previously these were only added to public.* by
-- the back office at startup, so app.transactions / app.merchants lacked them
-- and any portal query touching those entities failed.
--
-- Additive and idempotent: nullable columns only, no data is rewritten.
-- Rollback (only if no code depends on them):
--   alter table app.transactions drop column settlement_status, drop column settlement_reference,
--     drop column settlement_reason, drop column settlement_updated_at;
--   alter table app.merchants drop column lenco_transfer_recipient_id, drop column payout_method,
--     drop column payout_operator, drop column payout_phone, drop column payout_bank_id,
--     drop column payout_account_number;

alter table app.transactions
    add column if not exists settlement_status varchar(32),
    add column if not exists settlement_reference varchar(128),
    add column if not exists settlement_reason varchar(255),
    add column if not exists settlement_updated_at timestamp;

alter table app.merchants
    add column if not exists lenco_transfer_recipient_id varchar(64),
    add column if not exists payout_method varchar(32),
    add column if not exists payout_operator varchar(16),
    add column if not exists payout_phone varchar(20),
    add column if not exists payout_bank_id varchar(64),
    add column if not exists payout_account_number varchar(64);
