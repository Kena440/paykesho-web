-- Harden a pre-existing SECURITY DEFINER helper that should not be callable
-- through the public Supabase Data API.
revoke execute on function public.rls_auto_enable() from public, anon, authenticated;
