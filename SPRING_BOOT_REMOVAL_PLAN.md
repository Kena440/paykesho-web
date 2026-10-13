# PayKesho: Spring Boot removal and Cloudflare-native migration

## Goal

Remove Spring Boot and Java runtime dependencies from the PayKesho production request path. Keep Cloudflare Workers as the edge/router and Supabase PostgreSQL as the system of record. Do not deploy a partially migrated system.

## Current state verified on 2026-10-13

- `src/index.ts` routes traffic to three Cloudflare Container Durable Objects: back office, client portal, and payment service.
- All three container images are built from Maven projects and run Java 17/Spring Boot.
- The back office includes a large set of server-rendered Thymeleaf pages and controller/service/repository logic. The client portal and payment service also contain Java application logic.
- `PAYKESHO_DB_URL` is currently a JDBC URL. Cloudflare Workers cannot use that JDBC connection string directly.
- The existing back office schema fix PR #26 is not merged. It must not be treated as a completed production fix.

## Target architecture

- Cloudflare Worker (TypeScript): routing, request validation, rate limiting, security headers, and API handlers.
- Supabase Auth: user/admin sessions and identity; admin privileges must be checked server-side on every protected route.
- Supabase PostgreSQL: existing data remains the source of truth. Use explicit least-privilege database access and preserve the existing `app` schema. Do not expose a service-role key to browser code.
- Supabase Storage or Cloudflare R2: private uploads with signed/authorized access; do not rely on container-local disk.
- Lenco integration: server-side Worker endpoints only; validate webhook signatures and make payment/settlement processing idempotent.
- Static front-end assets served by Cloudflare Pages/Worker assets, not Java templates.

## Migration sequence

### Phase 1 — inventory and contracts
- Enumerate every externally used page, API route, scheduled task, integration, database table, and permission rule in the Java services.
- Map current Java DTOs and behavior to explicit TypeScript request/response contracts.
- Identify the Supabase API schema exposure and current authentication model before choosing the data access method.

### Phase 2 — worker-native foundation
- Added the first administrator-session slice at `/api/native/admin/login`, `/api/native/admin/session`, and `/api/native/admin/logout`. It uses a signed, eight-hour, Secure/HttpOnly/SameSite=Strict cookie and requires a session-signing secret.
- The native login now supports both the configured bootstrap administrator (`PAYKESHO_ADMIN_EMAIL` / `PAYKESHO_ADMIN_PASSWORD`) and database-backed administrator accounts. Database login checks the stored BCrypt hash, account enabled/deleted/locked flags and active `Admin`/`SuperAdmin` role membership. It still does **not** replace the legacy `/admin` routes or migrate password-reset/remember-me flows; keep those routes on the existing service until full parity is implemented.
- Configure `PAYKESHO_SESSION_SECRET` as a Cloudflare Worker secret with at least 32 characters, and `SUPABASE_SERVICE_ROLE_KEY` as a separate Worker secret. Never paste either secret into Git or browser code.
- Add a TypeScript API module, structured error handling, request IDs, health endpoints, and tests.
- Add server-only Supabase configuration. Never fall back to permissive authorization if a secret is absent.
- Add authentication/authorization middleware and audit logging before migrating administrative mutations.

### Phase 3 — back office
- Port login/session handling, dashboard, clients, merchants, organizations, transactions, vouchers, repayments, settlements, notifications, settings, user administration, and audit pages.
- Rebuild the current Thymeleaf pages as static/client-rendered pages backed by protected Worker APIs.
- Verify admin role enforcement and audit trails before switching `/admin` traffic.

### Phase 4 — client portal and payments
- Port registration, login, profile/KYC, payslip upload, eligibility/advance decisions, voucher/e-value flows, merchant QR payments, balances/ledger, and repayment views.
- Port Lenco initiation, webhook handling, and settlement flows with signature validation, idempotency, and reconciliation tests.

### Phase 5 — remove Java deployment
- Replace all three Container Durable Object bindings and remove Java/Maven Docker build steps only after parity tests pass.
- Remove Spring-specific environment variables, health checks, and startup-log code.
- Deploy to a preview environment; run end-to-end tests against a non-production test account/database first.
- Promote to production only after client, admin, payment, webhook, and rollback checks pass.

## Safety rules

- Do not drop or recreate database tables during this migration.
- Do not change financial calculations, balances, repayment schedules, eligibility, or settlement semantics without explicit tests and review.
- Do not disable authentication/RLS to make the new app work.
- Do not switch production routing to incomplete replacement handlers.
- Keep the existing production branch untouched until the replacement is tested.


## Latest migration increment

- Added session-protected, read-only paginated/searchable merchant and organization directory endpoints. Merchant payout routing and account fields are excluded from the response.
- Extended native login to verify database-backed administrator BCrypt hashes and role membership while preserving bootstrap-admin precedence.


## Read-only transaction API increment

- Added `GET /api/native/admin/transactions` with signed-admin session protection, pagination, text search, settlement-status filtering and settled/unsettled filtering. This endpoint is read-only; payment and settlement mutations remain on the existing services until idempotency, reconciliation and parity checks are in place.
