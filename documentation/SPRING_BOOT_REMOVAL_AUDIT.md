# PayKesho Spring Boot removal — database and cutover audit

Audit date: 2026-10-13. Read-only inspection of Supabase project `fjfgjfprhtdanjohabuj`; no database changes were made.

## Findings

- The Supabase project reports `ACTIVE_HEALTHY`.
- Application tables are in schema `app` (34 tables reported by the security advisor).
- RLS is enabled on the application tables, but the advisor reports no row-level policies on those tables.
- PostgreSQL reports no `USAGE` privilege on schema `app` for `anon`, `authenticated`, or `service_role`. The inspected application tables also have no table grants for those API roles.
- Therefore, the current application schema is not accessible to the Worker through ordinary Supabase Data API calls in its present privilege configuration. The existing JDBC/Spring Boot connection uses a different database access path.
- The legacy model separates `app.clients` from `app.users`; a Supabase Auth user is not automatically a PayKesho client or a legacy admin. Do not treat a successful Supabase Auth signup as proof of a linked customer profile or credit eligibility.
- Existing schema includes financial records and controls such as balances, credit scores, transactions, vouchers, repayments, settlements, payments, and idempotency records. They must be reused through verified business rules, not recreated as a second ledger.

## Security advisor observations

The Supabase security advisor reported 34 `rls_enabled_no_policy` findings. These are informational for the current private-schema design, but they must be resolved intentionally before any application schema is exposed to API roles. RLS alone is not a complete authorization model; a table needs the right grants and policies for the roles that access it.

The performance advisor also reported unindexed foreign keys. These are not the first migration blocker, but should be reviewed before significant production traffic.

- RLS advisor guidance: https://supabase.com/docs/guides/database/database-linter?lint=0008_rls_enabled_no_policy
- Data API exposure guidance: https://supabase.com/docs/guides/api/securing-your-api.md

## Required database access decision

Before implementing profile/KYC or financial endpoints, choose and test one controlled server-side access pattern:

1. Keep `app` private and implement narrowly scoped database functions in a separate API schema, exposing only those functions to the server role; or
2. Expose `app` deliberately to the Data API while granting access only to a server-only role and adding explicit authorization in every Worker route.

Do not grant `anon` or `authenticated` broad access to `app`. Do not put a service-role key in browser code. Prefer atomic PostgreSQL functions/transactions for balance, voucher, repayment, settlement and ledger changes. Financial writes must be idempotent and auditable.

## Cutover gates

- Confirm Auth identity mapping to existing `app.clients` and `app.users`; preserve legacy records and passwords.
- Verify private KYC/document storage and access checks.
- Implement and test advance eligibility, voucher issuance/redemption, merchant payments, Lenco webhook signatures, payout failure/refund behavior, pending/uncertain outcomes, reconciliation, notifications and admin authorization.
- Run tests for duplicate requests, replayed webhooks, retries, concurrent spending, unauthorized cross-account access and rollback.
- Deploy only to staging first. Keep `master` and production unchanged until all gates pass.

## Legacy payment behavior to preserve during replacement

Read-only review of the existing customer/payment code on `feature/paykesho-customer-journey` identified these behaviors:

- A payment requires a positive amount, a valid merchant, a configured/valid merchant payout profile, and an approved valid client voucher with enough available e-value.
- Voucher ownership must match the authenticated client. The existing JPA service uses a pessimistic row lock and consumes voucher value in the same database transaction that creates the PayKesho transaction, preventing concurrent QR payments from spending the same balance.
- New transactions use currency `ZMW` and begin with settlement status `PENDING`. Lenco is the external settlement rail; a successful HTTP request alone is not sufficient to mark a transfer successful.
- A confirmed Lenco failure refunds the voucher value once. Duplicate or out-of-order settlement events must not refund a successful payment or re-mark a refunded payment as successful. Ambiguous payout outcomes stay `PENDING` and are reconciled rather than risking a double spend/refund.
- The existing Lenco webhook validates `X-Lenco-Signature` against the raw request payload using HMAC-SHA512 and a key derived from the SHA-256 digest of the configured API token, then compares signatures in constant time. Preserve this exact protocol only after confirming it against current Lenco documentation and a real signed test fixture.
- A scheduled reconciliation job revisits `PENDING` transfers and applies only terminal `SUCCESSFUL` or `FAILED` outcomes.
- The existing customer endpoint uses idempotency records with request hashes and a 24-hour expiry. The replacement should retain idempotency, but strengthen the legacy behavior: never proceed with a financial mutation if the idempotency record cannot be reserved, and reject reuse of the same key with a different request hash.

These rules should become executable integration tests and atomic PostgreSQL functions before the 501 financial routes are enabled. Notifications and balance summaries should follow committed transaction state; they must not be the source of truth for money movement.

## Legacy customer registration fields

The legacy `app.clients` entity requires more than an email/password: phone number, address, TPIN, MAN number, name, email, NRC and client income are non-null fields; TPIN, MAN number, email and NRC are unique. The legacy client model also carries enablement/lock state and optional organization/file references. Supabase Auth signup alone therefore must not create or imply a complete PayKesho client. The migration preview collects only name, phone and email for Auth and intentionally does not create a legacy client row or authorize credit.
