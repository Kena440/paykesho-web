# PayKesho Cloudflare deployment

This branch adds a Cloudflare Workers + Containers deployment for the PayKesho client web portal.

## Architecture

Browser -> Cloudflare Worker -> Cloudflare Container -> Spring Boot/Thymeleaf client portal -> Supabase PostgreSQL

Cloudflare Containers requires the Worker Paid plan. The production deployment command is `npx wrangler deploy`.

## Required Worker secrets

Set these with Wrangler or in Cloudflare Workers > Settings > Variables and Secrets:

- `SPRING_DATASOURCE_URL` - Supabase PostgreSQL connection string
- `SPRING_DATASOURCE_USERNAME` - Supabase database username
- `SPRING_DATASOURCE_PASSWORD` - Supabase database password
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `FACEBOOK_CLIENT_ID`
- `FACEBOOK_CLIENT_SECRET`

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

The original PayKesho codebase was MySQL-based. This branch adds the PostgreSQL JDBC driver and a Cloudflare/Supabase Spring profile, but the application schema and SQL must still be verified against the target Supabase database before production traffic is enabled.

## Production CI/CD

Connect `Kena440/paykesho-web` to Cloudflare Workers Builds and use `master` as the production branch. The deploy command must be `npx wrangler deploy` so Cloudflare publishes the container image and rolls out the container, rather than using `wrangler versions upload`.
