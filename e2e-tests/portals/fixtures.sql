-- CI-only test accounts for the portal integration suite. Loaded into a
-- throwaway Postgres; never run against Supabase.
-- Every account's password is: Portal-Test-2026!
\set pw '''$2a$10$2TXrkm2BXrkjvGwUVB7Kt./jLHzVK45oyGcoEGz24J4QQSyL/W.QW'''

insert into app.role (uuid, name, description) values
  ('00000000-0000-0000-0000-00000000r001', 'Admin', 'Administrator'),
  ('00000000-0000-0000-0000-00000000r002', 'SuperAdmin', 'Super administrator');

insert into app.users (uuid, email, password, first_name, last_name, enabled, account_non_locked)
values ('00000000-0000-0000-0000-00000000u001', 'admin@ci.paykesho.test', :pw, 'CI', 'Admin', true, true);
insert into app.user_roles (user_id, role_id)
select u.id, r.id from app.users u, app.role r where u.email = 'admin@ci.paykesho.test' and r.name = 'Admin';

insert into app.organization_admins (uuid, phone_number, title, name, email, password, enabled, account_non_locked, created_time)
values ('00000000-0000-0000-0000-00000000o001', '260970000001', 'HR Manager', 'CI Employer Admin', 'employer@ci.paykesho.test', :pw, true, true, now());
insert into app.organizations (uuid, name, email, phone_number, contact_person_id, enabled, registration_date, organization_type)
select '00000000-0000-0000-0000-00000000o101', 'XYZ Limited (CI)', 'hr@xyz.test', '260970000001', id, true, now(), 'HEALTHCARE'
from app.organization_admins where email = 'employer@ci.paykesho.test';

insert into app.merchant_admins (uuid, phone_number, title, name, email, password, enabled, account_non_locked, created_time)
values ('00000000-0000-0000-0000-00000000m001', '260970000002', 'Owner', 'CI Merchant Owner', 'merchant@ci.paykesho.test', :pw, true, true, now());
insert into app.merchants (uuid, code, name, details, email, phone_number, contact_person_id, enabled, registration_date, merchant_type)
select '00000000-0000-0000-0000-00000000m101', 'CIMERCH001', 'CI Grocers', 'Groceries', 'shop@ci.paykesho.test', '260970000002', id, true, now(), 'GROCERY_STORE'
from app.merchant_admins where email = 'merchant@ci.paykesho.test';

insert into app.clients (uuid, phone_number, address, code, tpin, man_number, name, email, nrc, password, enabled, account_non_locked, created_time, organization_id, client_type)
select '00000000-0000-0000-0000-00000000c001', '260970000003', 'Plot 1, Lusaka', 'CICLIENT001', '1000000001', '1001', 'Thomas CI', 'client@ci.paykesho.test', '111111/11/1', :pw, true, true, now(), id, null
from app.organizations where name = 'XYZ Limited (CI)';

-- Employer staff list entry for the registration test (employee not yet registered).
insert into app.employee_records (uuid, name, nrc, tpin, man_number, organization_id, registration_date)
select '00000000-0000-0000-0000-00000000e001', 'Mary Banda', '222222/22/2', '2000000002', '1002', id, now()
from app.organizations where name = 'XYZ Limited (CI)';

-- Merchant payout profile, so a client payment can settle (to the Lenco test
-- double in CI: scripts/lenco-stub.mjs).
update app.merchants set payout_method = 'mobile-money', payout_operator = 'airtel', payout_phone = '0970000002'
where code = 'CIMERCH001';
