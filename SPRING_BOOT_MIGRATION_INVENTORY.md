# PayKesho Spring Boot replacement inventory

Snapshot date: 2026-10-13. Source of truth: `master` before the Worker-native migration branch.

## Cloudflare request path today

`src/index.ts` routes:
- `/admin/**` to the back-office container
- `/webhooks/lenco` and `/internal/payment/**` to the payment-service container
- all other paths to the client-portal container

All three containers are Java 17 / Maven builds. The Worker-native health routes are being introduced alongside the old routes; this is not yet a full replacement.

## Back-office modules to port

| Domain | Existing controller / service area | Target migration notes |
| --- | --- | --- |
| Authentication | `adminauth/AdminAuthController`, `AdminAuthSecurityConfig`, `AdminUserDetailsService`, `security/*` | Highest priority. Replace form login, sessions, logout, reset flows and role checks. Do not port the hard-coded remember-me signing key. |
| Dashboard | `MainController`, `dashboard/service/DashboardAnalyticsService` | Preserve current metric names and calculations; verify counts and amounts against SQL fixtures. |
| Transactions | `transaction/TransactionController`, `admin/AdminRestController` | Read-only listing/summary first; mutations and settlement status require audit trail and idempotency. |
| Vouchers | `voucher/VoucherController`, `voucher/VoucherService` | Approval triggers repayment schedules and notifications; must preserve idempotency and transactional behavior. |
| Clients | `client/ClientController`, `client/ClientRestController` | PII, balances, credit scores, payroll deductions and payslips require per-role access checks. |
| Merchants | `merchants/*`, `merchantadmin/*` | Include payout configuration and settlement permissions. |
| Organizations | `organizations/*`, `organizationadmin/*` | Scope access to authorized organization records. |
| Repayments | `repayments/RepaymentsController`, `repayments/RepaymentService` | Financial calculations require parity tests and explicit decimal handling. |
| Settlements | `settlement/SettlementController`, `SettlementService` | Keep reconciliation and Lenco transfer handling server-side and idempotent. |
| Files | `files/SecureFileUploadController`, `UploadsRestController` | Move away from local container storage; use private object storage and short-lived signed URLs. |
| Notifications | `notifications/NotificationController`, `notification/*` | Preserve retry behavior; send through server-only providers. |
| Settings | `settings/SettingsController`, `GeneralRestController`, `CountryRestController`, `StateRestController` | Validate all writes and keep country/currency settings consistent. |
| Security audit | `security/controller/SecurityAuditController` | Maintain immutable actor/action/time records for sensitive actions. |
| Integrations | `integrations/IntegrationsController` | Never return provider credentials in responses. |
| System health | `monitoring/SystemHealthController` | Replace JVM/memory/disk probes with Worker and dependency health signals. |

Other controller files in the back-office tree include account/user management, forgot-password, auth test/debug, custom error, and merchant/organization administration endpoints. Inventory each route before switching traffic.

## Existing Supabase schema areas

The active PayKesho project contains the `app` schema with tables including:
- Identity and roles: `users`, `user_roles`, `role`, `organization_admins`, `merchant_admins`
- Customers and partners: `clients`, `merchants`, `organizations`, `employee_records`
- Financial operations: `transactions`, `payments`, `vouchers`, `client_balance`, `repayment_schedules`, `settlements`, `merchant_money_account_details`
- Support/security: `settings`, `countries`, `states`, `currencies`, `file_uploads`, `otps`, `notification_requests`, `idempotency_records`, `security_audits`

RLS is enabled on the observed app tables. Existing policy coverage is incomplete/deny-all for direct user roles, so do not expose the service-role key or switch the browser to direct table access. Worker server routes must enforce identity and authorization explicitly; RLS should remain defense-in-depth.

## Worker-native endpoints added on the migration branch

- `GET /api/native/health`: Worker liveness; does not contact a container.
- `GET /api/native/db-health`: server-side readiness probe against `app.settings`; requires the service-role Worker secret and Supabase Data API exposure for the `app` schema.
- `POST /api/native/admin/login`, `GET /api/native/admin/session`, `POST /api/native/admin/logout`: signed eight-hour cookie sessions. Login supports the configured bootstrap administrator and database-backed administrator users after checking enabled/deleted/locked flags, BCrypt password hash and an active `Admin`/`SuperAdmin` role. This does not yet replace the legacy `/admin` pages or migrate password-reset/remember-me flows; do not treat it as complete authentication parity.
- `GET /api/native/admin/dashboard`: session-protected read-only counts for organizations, merchants, clients, transactions, and vouchers, including active/inactive counts for organizations, merchants, and clients. It returns aggregate counts only; transaction values, trends, top performers, and UI parity remain to be migrated.
- `GET /api/native/admin/clients`: session-protected, paginated/searchable read-only client directory. Returns only ID, UUID, code, name, email, phone, enabled status, creation time and organization ID; omits passwords, NRC, TPIN, verification/reset tokens and balances.
- `GET /api/native/admin/merchants` and `GET /api/native/admin/organizations`: session-protected, paginated/searchable read-only directories with active/inactive filtering. Merchant responses intentionally omit Lenco recipient IDs, payout phone, bank identifiers and account numbers; neither endpoint supports writes.
- `GET /api/native/admin/transactions`: session-protected, paginated/searchable read-only transaction listing with optional settled/unsettled filtering. Its selected fields were aligned to the live `app.transactions` schema; settlement status/reference columns are not present in that schema and are not selected. It exposes transaction amounts and settlement references only to the signed administrator session; it has no mutation actions.
- Non-GET requests to the health endpoints return 405.

## Migration order

1. Worker-native liveness/readiness and safe Supabase REST adapter.
2. Authentication and session design; verify existing identity/password model before migrating.
3. Dashboard read endpoints and parity checks.
4. Read-only client, merchant, organization and transaction APIs.
5. Financial mutations (voucher approvals, repayments, settlements, payments) with idempotency/reconciliation tests.
6. Static UI migration away from Thymeleaf templates.
7. Client portal, payment service and notification/file dependencies.
8. Remove all Java Container Durable Object bindings and Docker/Maven build steps only after parity tests pass.
