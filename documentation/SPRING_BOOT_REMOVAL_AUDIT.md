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
