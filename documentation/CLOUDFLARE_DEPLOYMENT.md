# PayKesho Cloudflare Containers Deployment

PayKesho is a multi-module Spring Boot application. Cloudflare Containers runs one Spring Boot module per Container class while a single Worker provides the gateway.

Public routes:
- /client/* -> client portal
- /merchant/* -> merchant portal
- /organization/* -> organization portal
- /admin/* -> backoffice
- /payments/* -> payment service
- /notifications/* -> notification service
- /files/* -> file service
- /health -> Worker health

Internal service calls are designed to use Cloudflare Container outbound virtual hosts such as http://notification.internal and http://file.internal, avoiding public service endpoints.

## Production blockers
1. Container disk is ephemeral. The current file-service stores uploads on local disk. File persistence must be moved to Cloudflare R2 before production file uploads.
2. The Supabase branch still uses Hibernate `ddl-auto=update`. Verify the PostgreSQL schema and then change production to `validate`.
3. Payment, SMTP, OAuth and database credentials must be Cloudflare secrets and must never be committed.
4. The current Docker Compose service names do not exist on Cloudflare. Service-to-service URLs must be changed to the internal virtual hosts.

## Local validation
Prerequisites: Docker Desktop, Node.js 20+, npm.

Run:
`npm install`
`npx wrangler types`
`npx wrangler deploy --dry-run`

First real deployment:
`npx wrangler deploy`

After deployment:
`npx wrangler containers list`
`npx wrangler containers images list`

## Workers Builds
Connect the repository in Cloudflare Workers Builds.
Production deploy command: `npx wrangler deploy`.
Root directory: repository root.
Build command: leave empty.
Do not use `wrangler versions upload` for the production container deployment because it does not publish container images.

## Custom domains
After the first healthy deployment, attach the required domain and then add hostname-based routing if desired.
