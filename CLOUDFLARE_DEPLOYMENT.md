# PayKesho Cloudflare deployment

This branch adds a Cloudflare Workers + Containers deployment for the PayKesho client web portal.

## Architecture

Browser -> Cloudflare Worker -> Cloudflare Container -> Spring Boot/Thymeleaf client portal -> Supabase PostgreSQL

Cloudflare Containers requires the Worker Paid plan. The production deployment command is `npx wrangler deploy`.

## Required Worker secrets

Set these with Wrangler or in Cloudflare Workers > Settings > Variables and Secrets:

- `PAYKESHO_DB_URL` - Supabase PostgreSQL JDBC connection string (use Session pooler/direct connection, port 5432, with `sslmode=require`)
- `PAYKESHO_DB_USERNAME` - Supabase database username
- `PAYKESHO_DB_PASSWORD` - Supabase database password

Google and Facebook OAuth remain disabled in the Cloudflare profile until configured.

Do not commit these values.

## Optional variables

- `NOTIFICATION_SERVICE_URL`
- `FILE_SERVICE_URL`
- `RATE_LIMITING_ENABLED` (defaults to `true`)

## Local checks

1. Install Node.js and Docker Desktop.
2. Run `npm install`.
3. Run `npm run typecheck`.
4. Authenticate with Cloudflare using `npx wrangler login`.
5. Set the required secrets.
6. Run `npx wrangler deploy`.

## Important database note

The original PayKesho codebase was MySQL-based. This branch adds the PostgreSQL JDBC driver and a Cloudflare/Supabase Spring profile, but the Cloudflare profile now targets the Supabase PostgreSQL `app` schema. The `supabase/migrations/20260927140000_create_paykesho_app_schema.sql` migration creates that schema; Hibernate then creates/updates the mapped PayKesho tables on application startup.

## Supabase connection

Use the Supabase Session pooler on port `5432` for this persistent Spring/JPA container, or the direct connection when IPv6 is available. Do not use the transaction pooler on port `6543` for Hibernate/JPA.

## Production CI/CD

Connect `Kena440/paykesho-web` to Cloudflare Workers Builds and use `master` as the production branch. The deploy command must be `npx wrangler deploy` so Cloudflare publishes the container image and rolls out the container, rather than using `wrangler versions upload`.
