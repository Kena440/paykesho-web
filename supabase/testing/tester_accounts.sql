-- Test-stage accounts for the live site (approved by Amukena, 26 Oct 2026:
-- demo merchant and demo employer accounts in production).
--
-- Creates, only if missing:
--   * employer  "PayKesho Demo Employer (DEMO)"  - employer portal /employer
--   * merchant  "PayKesho Demo Shop" (code M20261DEMO1) - merchant portal /merchant
--   * one staff-list entry at the demo employer, so a tester can register as a
--     client at / (Register) with: name Demo Employee, NRC 999999/99/1,
--     TPIN 9000000001, man number DEMO-001.
-- Both portal accounts sign in with the back-office administrator's email and
-- password (the password hash is copied; no new password is created).
--
-- The demo merchant gets NO payout profile: until Lenco is pointed at its
-- sandbox, a payment to it stops with "Merchant payout method is not
-- configured" instead of moving real money.
--
-- Idempotent: safe to run more than once. Remove with the statements at the end.

begin;

insert into app.organization_admins (uuid, phone_number, title, name, email, password, enabled, account_non_locked, created_time)
select gen_random_uuid()::text, '260970000101', 'HR Manager', 'Demo Employer Admin', u.email, u.password, true, true, now()
from app.users u
where u.email = 'admin@paykesho.com'
  and not exists (select 1 from app.organization_admins a where a.email = u.email);

insert into app.organizations (uuid, name, email, phone_number, contact_person_id, enabled, registration_date, organization_type)
select gen_random_uuid()::text, 'PayKesho Demo Employer (DEMO)', 'demo-employer@paykesho.test', '260970000101', a.id, true, now(), 'TECHNOLOGY'
from app.organization_admins a
where a.email = 'admin@paykesho.com'
  and not exists (select 1 from app.organizations o where o.name = 'PayKesho Demo Employer (DEMO)');

insert into app.employee_records (uuid, name, nrc, tpin, man_number, organization_id, registration_date)
select gen_random_uuid()::text, 'Demo Employee', '999999/99/1', '9000000001', 'DEMO-001', o.id, now()
from app.organizations o
where o.name = 'PayKesho Demo Employer (DEMO)'
  and not exists (select 1 from app.employee_records e where e.man_number = 'DEMO-001' and e.organization_id = o.id);

insert into app.merchant_admins (uuid, phone_number, title, name, email, password, enabled, account_non_locked, created_time)
select gen_random_uuid()::text, '260970000102', 'Owner', 'Demo Shop Owner', u.email, u.password, true, true, now()
from app.users u
where u.email = 'admin@paykesho.com'
  and not exists (select 1 from app.merchant_admins m where m.email = u.email);

insert into app.merchants (uuid, code, name, details, email, phone_number, contact_person_id, enabled, registration_date, merchant_type)
select gen_random_uuid()::text, 'M20261DEMO1', 'PayKesho Demo Shop', 'Demo groceries (test only)', 'demo-shop@paykesho.test', '260970000102', m.id, true, now(), 'GROCERY_STORE'
from app.merchant_admins m
where m.email = 'admin@paykesho.com'
  and not exists (select 1 from app.merchants x where x.code = 'M20261DEMO1');

commit;

-- Check:
--   select 'employer' k, count(*) from app.organizations where name = 'PayKesho Demo Employer (DEMO)'
--   union all select 'merchant', count(*) from app.merchants where code = 'M20261DEMO1'
--   union all select 'staff', count(*) from app.employee_records where man_number = 'DEMO-001';
--
-- Remove (only once no test vouchers/transactions reference them):
--   delete from app.employee_records where man_number = 'DEMO-001';
--   delete from app.merchants where code = 'M20261DEMO1';
--   delete from app.organizations where name = 'PayKesho Demo Employer (DEMO)';
--   delete from app.merchant_admins where email = 'admin@paykesho.com';
--   delete from app.organization_admins where email = 'admin@paykesho.com';
