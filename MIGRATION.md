# PayKesho Spring Boot replacement

Migration foundation only; not production cutover ready. Existing Spring Boot remains on master until all workflows are migrated and tested.

Required Worker secrets: SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_ROLE_KEY. Service-role key is server-side only.

Implemented: GET /api/health, POST /api/auth/signup, POST /api/auth/signin, GET /api/auth/user. Financial routes intentionally return 501 until authorization, atomic transactions, idempotency, ledger postings, voucher issuance, merchant settlement and reconciliation are implemented.

Next: inspect current schema and business rules; migrate KYC/documents; implement advances and ledger/voucher/merchant postings; migrate Lenco callbacks, notifications and admin; staging tests before cutover.