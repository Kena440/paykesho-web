-- Phase 0 (approved by Amukena, 19 Oct 2026): keep schema "app" server-side only.
-- The Java services connect as the database owner and are not affected by any of this.

-- 1. Signed-in API users must not call the SECURITY DEFINER helper.
revoke execute on function app.is_backoffice_admin() from public, anon, authenticated;

-- 2. Nothing in schema app is reachable through the Supabase REST/GraphQL API.
revoke usage on schema app from public, anon, authenticated;
revoke all on all tables in schema app from public, anon, authenticated;
revoke all on all sequences in schema app from public, anon, authenticated;
revoke all on all functions in schema app from public, anon, authenticated;
alter default privileges for role postgres in schema app revoke all on tables from public, anon, authenticated;
alter default privileges for role postgres in schema app revoke all on sequences from public, anon, authenticated;
alter default privileges for role postgres in schema app revoke all on functions from public, anon, authenticated;

-- 3. Explicit deny-all RLS policy on every table, so the intent is recorded and
--    any future grant cannot expose rows by accident.
do $$
declare t record;
begin
  for t in select tablename from pg_tables where schemaname = 'app' loop
    execute format('alter table app.%I enable row level security', t.tablename);
    execute format('drop policy if exists deny_api_roles on app.%I', t.tablename);
    execute format(
      'create policy deny_api_roles on app.%I as restrictive for all to anon, authenticated using (false) with check (false)',
      t.tablename);
  end loop;
end $$;
