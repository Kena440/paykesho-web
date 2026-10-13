# PayKesho Spring Boot replacement

This branch is a staged migration, not a production cutover. The existing Spring Boot deployment on `master` remains untouched until the replacement has passed end-to-end staging tests.

## Current migration slice

- Native TypeScript Cloudflare Worker; no Spring Boot container proxy.
- Static responsive PayKesho landing page and account panel served as Worker assets.
- Supabase Auth endpoints:
  - `GET /api/health`
  - `POST /api/auth/signup`
  - `POST /api/auth/signin`
  - `GET /api/auth/user`
  - `POST /api/auth/signout`
  - `POST /api/auth/password-reset`
- Worker security headers and GitHub Actions type-check CI.

Required Worker secrets: `SUPABASE_URL`, `SUPABASE_ANON_KEY`. These can be set as Cloudflare secrets; never place a service-role key in frontend code. The current auth-only slice does not require a service-role key.

## Explicitly not migrated

Financial endpoints intentionally return `501 workflow_not_migrated`: advances, vouchers, merchant payments and ledger. Customer-to-legacy-profile linking, KYC and document storage, credit assessment, transactional ledger postings, idempotency, voucher issuance, merchant settlement/reconciliation, Lenco callbacks, notifications and back-office administration still need migration and tests.

## Required before cutover

1. Define and test the Supabase Auth ↔ legacy `app.clients` / `app.users` identity-linking strategy; do not recreate or overwrite existing customer records or legacy password hashes.
2. Confirm Supabase Data API exposure/access for the `app` schema; its tables currently have RLS enabled and no row policies, so do not expose them directly to browser clients.
3. Implement financial mutations as atomic Postgres functions/transactions with authorization, idempotency and auditable ledger entries.
4. Migrate KYC/private file storage, Lenco webhook verification, notifications and admin functions.
5. Run staging tests for registration, existing-account migration, credit eligibility, payments, refunds, retries, reconciliation, permissions and failure handling.
6. Configure production secrets and approved origins, then perform a planned cutover. Do not merge/deploy this branch before the gates above pass.
