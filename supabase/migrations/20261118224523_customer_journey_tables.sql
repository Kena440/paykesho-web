-- PAYKESHO: tables the customer journey needs that the app schema does not have yet.
-- Adds new tables only. Does not change or drop any existing table or data.
-- No financial policy values (limits, rates, fees, chart of accounts) are seeded here.
-- Follows the Phase 0 lock-down: RLS on, no access for anon/authenticated API roles.


-- Step 2-3: registration KYC review (spec: back office approves before the account is active)
create table app.client_kyc_applications (
  id              bigint generated always as identity primary key,
  uuid            uuid not null default gen_random_uuid() unique,
  client_id       integer not null references app.clients(id),
  first_name      varchar(100) not null,
  middle_name     varchar(100),
  last_name       varchar(100) not null,
  house_number    varchar(50),
  road            varchar(150),
  area            varchar(150),
  status          varchar(20) not null default 'SUBMITTED'
                  check (status in ('SUBMITTED','UNDER_REVIEW','MORE_INFO_REQUIRED','APPROVED','REJECTED')),
  submitted_at    timestamptz not null default now(),
  reviewed_by     integer references app.users(id),
  reviewed_at     timestamptz,
  decision_reason text,
  updated_at      timestamptz not null default now(),
  check ((status in ('APPROVED','REJECTED')) = (reviewed_by is not null and reviewed_at is not null))
);
create index on app.client_kyc_applications (client_id);
create index on app.client_kyc_applications (status);

-- Step 2 / Step 4: NRC, payslip and selfie files linked to an application
create table app.kyc_documents (
  id                 bigint generated always as identity primary key,
  uuid               uuid not null default gen_random_uuid() unique,
  client_id          integer not null references app.clients(id),
  kyc_application_id bigint references app.client_kyc_applications(id),
  voucher_id         integer references app.vouchers(id),
  document_type      varchar(20) not null
                     check (document_type in ('NRC_FRONT','NRC_BACK','PAYSLIP','SELFIE','OTHER')),
  file_upload_id     integer not null references app.file_uploads(id),
  status             varchar(20) not null default 'PENDING'
                     check (status in ('PENDING','ACCEPTED','REJECTED')),
  created_at         timestamptz not null default now()
);
create index on app.kyc_documents (client_id);
create index on app.kyc_documents (kyc_application_id);

-- Step 8: transaction PIN, separate from login password; lockout after 3 wrong tries
create table app.client_transaction_pins (
  client_id       integer primary key references app.clients(id),
  pin_hash        varchar(255) not null,
  failed_attempts smallint not null default 0 check (failed_attempts between 0 and 3),
  locked_at       timestamptz,
  last_changed_at timestamptz not null default now(),
  check ((failed_attempts = 3) = (locked_at is not null))
);

-- Trusted devices (new device = OTP + selfie re-verification)
create table app.trusted_devices (
  id                 bigint generated always as identity primary key,
  client_id          integer not null references app.clients(id),
  device_fingerprint varchar(128) not null,   -- hash, never the raw identifier
  label              varchar(100),
  status             varchar(25) not null default 'PENDING_VERIFICATION'
                     check (status in ('PENDING_VERIFICATION','TRUSTED','REVOKED')),
  verified_at        timestamptz,
  revoked_at         timestamptz,
  last_seen_at       timestamptz,
  created_at         timestamptz not null default now(),
  unique (client_id, device_fingerprint)
);

-- Step 8: static QR per merchant, revocable and replaceable
create table app.merchant_qr_codes (
  id             bigint generated always as identity primary key,
  uuid           uuid not null default gen_random_uuid() unique,
  merchant_id    integer not null references app.merchants(id),
  qr_payload     varchar(255) not null unique,
  status         varchar(10) not null default 'ACTIVE' check (status in ('ACTIVE','REVOKED')),
  issued_at      timestamptz not null default now(),
  revoked_at     timestamptz,
  revoked_reason text,
  check ((status = 'REVOKED') = (revoked_at is not null))
);
create unique index merchant_qr_codes_one_active on app.merchant_qr_codes (merchant_id) where status = 'ACTIVE';

-- Step 5-6: advance decision with maker-checker (approver cannot be the person who proposed)
create table app.advance_decisions (
  id              bigint generated always as identity primary key,
  voucher_id      integer not null unique references app.vouchers(id),
  decision        varchar(10) not null check (decision in ('APPROVED','REJECTED')),
  approved_amount numeric(18,2) check (approved_amount > 0),
  proposed_by     integer not null references app.users(id),
  checked_by      integer references app.users(id),
  decided_at      timestamptz not null default now(),
  reason          text,
  check (checked_by is null or checked_by <> proposed_by),
  check ((decision = 'APPROVED') = (approved_amount is not null))
);

-- Integration points 1-2: double-entry ledger (structure only; chart of accounts and posting
-- rules need product-owner approval before any rows are written)
create table app.ledger_accounts (
  id         bigint generated always as identity primary key,
  code       varchar(30) not null unique,
  name       varchar(150) not null,
  type       varchar(10) not null check (type in ('ASSET','LIABILITY','EQUITY','INCOME','EXPENSE')),
  owner_type varchar(10) check (owner_type in ('CLIENT','MERCHANT','EMPLOYER','PLATFORM')),
  owner_id   integer,
  currency   char(3) not null default 'ZMW',
  active     boolean not null default true,
  created_at timestamptz not null default now()
);
create index on app.ledger_accounts (owner_type, owner_id);

create table app.journal_entries (
  id                  bigint generated always as identity primary key,
  reference           varchar(64) not null unique,
  idempotency_key     varchar(128) not null unique,
  event_type          varchar(40) not null,  -- e.g. ADVANCE_ISSUED, MERCHANT_PAYMENT, PAYOUT_FAILED_REVERSAL
  description         text,
  voucher_id          integer references app.vouchers(id),
  transaction_id      integer references app.transactions(id),
  reverses_journal_id bigint references app.journal_entries(id),
  created_by          integer references app.users(id),
  posted_at           timestamptz not null default now()
);

create table app.ledger_postings (
  id         bigint generated always as identity primary key,
  journal_id bigint not null references app.journal_entries(id),
  account_id bigint not null references app.ledger_accounts(id),
  direction  char(1) not null check (direction in ('D','C')),
  amount     numeric(18,2) not null check (amount > 0)
);
create index on app.ledger_postings (journal_id);
create index on app.ledger_postings (account_id);

-- Every journal must balance (debits = credits, at least 2 lines) at commit time
create function app.check_journal_balanced() returns trigger
language plpgsql set search_path = '' as $$
declare d numeric; c numeric; n int;
begin
  select coalesce(sum(amount) filter (where direction='D'),0),
         coalesce(sum(amount) filter (where direction='C'),0), count(*)
    into d, c, n
    from app.ledger_postings where journal_id = new.journal_id;
  if n < 2 or d <> c then
    raise exception 'journal % unbalanced: debits % credits % lines %', new.journal_id, d, c, n;
  end if;
  return null;
end $$;
create constraint trigger ledger_postings_balanced
  after insert on app.ledger_postings
  deferrable initially deferred for each row execute function app.check_journal_balanced();

-- Posted ledger rows are immutable; corrections are reversing journals
create function app.block_ledger_changes() returns trigger
language plpgsql set search_path = '' as $$
begin
  raise exception 'ledger rows are immutable; post a reversing journal instead';
end $$;
create trigger journal_entries_immutable before update or delete on app.journal_entries
  for each row execute function app.block_ledger_changes();
create trigger ledger_postings_immutable before update or delete on app.ledger_postings
  for each row execute function app.block_ledger_changes();

-- Lenco (and later provider) webhook events, de-duplicated
create table app.provider_events (
  id              bigint generated always as identity primary key,
  provider        varchar(20) not null,
  event_id        varchar(128) not null,
  event_type      varchar(60) not null,
  reference       varchar(64),
  payload         jsonb not null,
  signature_valid boolean not null,
  received_at     timestamptz not null default now(),
  processed_at    timestamptz,
  unique (provider, event_id)
);
create index on app.provider_events (reference);

-- Lock-down, same as the rest of the app schema
do $$
declare t text;
begin
  foreach t in array array['client_kyc_applications','kyc_documents','client_transaction_pins',
    'trusted_devices','merchant_qr_codes','advance_decisions','ledger_accounts','journal_entries',
    'ledger_postings','provider_events'] loop
    execute format('alter table app.%I enable row level security', t);
    execute format('revoke all on app.%I from anon, authenticated', t);
    execute format('create policy deny_api_roles on app.%I as restrictive for all to anon, authenticated using (false) with check (false)', t);
  end loop;
end $$;
revoke all on function app.check_journal_balanced() from public, anon, authenticated;
revoke all on function app.block_ledger_changes() from public, anon, authenticated;

