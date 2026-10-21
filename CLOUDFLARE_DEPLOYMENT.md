# PayKesho Cloudflare deployment

## Architecture

One site, one Worker, one Spring Boot container per portal, one database:

| Path | Container | Spring context path |
|---|---|---|
| `/` (everything not below) | client portal | `/` |
| `/admin/**` | back office | `/admin` |
| `/merchant/**` | merchant portal | `/merchant` |
| `/employer/**` | employer (organization) portal | `/employer` |
| `/webhooks/lenco`, `/internal/payment/internal/v1/**` | payment service | `/` (prefix stripped) |

Every portal uses the Supabase PostgreSQL `app` schema (`currentSchema=app`).
The routing table lives in `src/routing.ts` and is shared by the Worker and the
CI router (`scripts/portal-router.mjs`). Legacy `/internal/payment/rest/api/**`
endpoints are never routable.

The Worker forwards the public host and scheme (`X-Forwarded-Host/Proto/Port`)
so redirects and cookies use the real site address; each portal has its own
session and remember-me cookie name (`PK_CLIENT_SESSION`, `PK_ADMIN_SESSION`,
`PK_MERCHANT_SESSION`, `PK_EMPLOYER_SESSION`).

Cloudflare Containers requires the Worker Paid plan. Deploy with `npx wrangler deploy`.

## Worker secrets

Required:

- `PAYKESHO_DB_URL` - Supabase JDBC URL (Session pooler, port 5432, `sslmode=require`)
- `PAYKESHO_DB_USERNAME`, `PAYKESHO_DB_PASSWORD`
- `PAYKESHO_ADMIN_EMAIL`, `PAYKESHO_ADMIN_PASSWORD` - if no back-office user with
  this email exists, one is created with the Admin role on start-up. An
  existing user is never changed; sign-in always uses the stored password.

Also passed through to the containers when set:

- `REMEMBER_ME_KEY` - shared signing key for "remember me" cookies (without it
  users are signed out whenever a container restarts)
- `PAYKESHO_INTERNAL_TOKEN`, `LENCO_API_TOKEN`, `LENCO_ACCOUNT_ID`
- Google/Facebook sign-in (off by default): `PAYKESHO_OAUTH2_ENABLED=true`,
  `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `FACEBOOK_CLIENT_ID`,
  `FACEBOOK_CLIENT_SECRET`, and `PAYKESHO_OAUTH2_EXCLUDE` set to an empty value.

Do not commit these values.

## Database

Each portal keeps at most 4 connections and releases idle ones after 60
seconds, because five applications share the Supabase pooler. Hibernate never
changes the schema (`ddl-auto=none`); schema changes are Supabase migrations in
`supabase/migrations`, applied before the release that needs them.

## Verification

The **Portals integration** workflow builds every module, starts all five
applications against Postgres loaded with the `app` schema snapshot
(`supabase/local/app_schema_snapshot.sql`) plus pending migrations, routes them
through the shared routing table, checks that every entity matches the schema,
and signs in to each portal with Playwright (`e2e-tests/portals`).

## Production CI/CD

Cloudflare Workers Builds deploys `master` with `npx wrangler deploy`.
