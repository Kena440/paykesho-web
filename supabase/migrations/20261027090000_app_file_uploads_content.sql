-- Keeps uploaded files (client payslips, merchant/organisation logos, staff
-- photos) in the database instead of the separate file service, which is not
-- deployed and stored files on a container disk that is wiped on restart.
--
-- app.file_uploads already holds each upload's metadata; this adds the bytes
-- and a SHA-256 checksum. The app schema has row-level security on and no
-- grants to the anon/authenticated API roles, so the files are reachable only
-- through the portals' authenticated /files/{id} endpoints.
--
-- Additive and idempotent: nullable columns only, no data is rewritten.
-- Rollback (only if no code depends on them):
--   alter table app.file_uploads drop column content, drop column sha256;

alter table app.file_uploads
    add column if not exists content bytea,
    add column if not exists sha256 varchar(64);
