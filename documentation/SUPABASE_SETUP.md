# PayKesho + Supabase PostgreSQL

This branch prepares PayKesho's Spring Boot services to use the Supabase PostgreSQL project:

- Project ref: `fjfgjfprhtdanjohabuj`
- Supabase schema used by Hibernate: `app`
- Database connection variable: `SUPABASE_DB_URL`

## Required deployment secret

Set `SUPABASE_DB_URL` to the **Session Pooler** JDBC connection string from Supabase Dashboard -> Connect.

Use port **5432** for Session Pooler. Do not use the Transaction Pooler (6543) as the main JDBC source for these Spring Boot/JPA services because transaction pooling does not support the prepared-statement/session behavior commonly used by ORMs.

The JDBC URL must include SSL, for example:

```
jdbc:postgresql://[POOLER-HOST]:5432/postgres?user=postgres.[PROJECT-REF]&password=[YOUR-PASSWORD]&sslmode=require
```

Do not commit the real URL or database password.

## Database schema

The Supabase project was verified as healthy and currently has no application tables. The `app` schema was created and verified.

PayKesho is currently configured with:

```
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.default_schema=app
```

This allows Hibernate to bootstrap the tables from the existing JPA entities when the application starts.

For production, the next step should be to convert the resulting PostgreSQL schema into versioned migrations and change `ddl-auto` to `validate`.

## Local setup

1. Copy `environment-template.env` to your local environment file.
2. Set `SUPABASE_DB_URL` using the Session Pooler JDBC URL from Supabase.
3. Set the OAuth, SMTP, ZynlePay and application secrets required by the service you are running.
4. Start the required Spring Boot service.
5. Verify the application's health endpoint and confirm that Hibernate can connect to the `app` schema.

## Security

The previous repository contained credentials directly in application property files. The Supabase integration branch replaces those values with environment variables.

Those previously committed credentials should be treated as compromised and rotated, especially:
- Google OAuth secret
- Facebook OAuth secret
- ZynlePay API credentials
- SMTP/Gmail password
- Notification credentials

## Important architecture note

Supabase provides PostgreSQL, while the original PayKesho application was configured for MySQL. This branch changes the runtime datasource and Hibernate dialect to PostgreSQL and adds the PostgreSQL JDBC driver.

The existing MySQL migration scripts have **not** been blindly executed against Supabase because they contain MySQL-specific SQL. The JPA entity model is being used for the initial schema bootstrap instead.

Before production cutover, run the full application test suite and review any MySQL-specific SQL, native queries, functions, indexes, and data types for PostgreSQL compatibility.
