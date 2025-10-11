-- Script to populate UUIDs for ALL existing records in the database
-- This explicitly generates UUIDs for every row that doesn't have one

-- =============================================================================
-- Disable safe mode temporarily
-- =============================================================================
SET SQL_SAFE_UPDATES = 0;

-- =============================================================================
-- Show initial state
-- =============================================================================
SELECT 'Starting UUID population for existing records...' AS Status;

-- Count records without UUIDs
SELECT 
    'users' as table_name,
    COUNT(*) as total_records,
    SUM(CASE WHEN uuid IS NULL THEN 1 ELSE 0 END) as records_without_uuid
FROM users
UNION ALL
SELECT 
    'merchants' as table_name,
    COUNT(*) as total_records,
    SUM(CASE WHEN uuid IS NULL THEN 1 ELSE 0 END) as records_without_uuid
FROM merchants
UNION ALL
SELECT 
    'organizations' as table_name,
    COUNT(*) as total_records,
    SUM(CASE WHEN uuid IS NULL THEN 1 ELSE 0 END) as records_without_uuid
FROM organizations
UNION ALL
SELECT 
    'clients' as table_name,
    COUNT(*) as total_records,
    SUM(CASE WHEN uuid IS NULL THEN 1 ELSE 0 END) as records_without_uuid
FROM clients;

-- =============================================================================
-- POPULATE UUIDs for existing records
-- Using primary key in WHERE to satisfy safe mode
-- =============================================================================

-- Users
UPDATE users 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM users WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Users: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Merchants
UPDATE merchants 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM merchants WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Merchants: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Organizations
UPDATE organizations 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM organizations WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Organizations: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Clients
UPDATE clients 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM clients WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Clients: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Vouchers
UPDATE vouchers 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM vouchers WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Vouchers: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Transactions
UPDATE transactions 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM transactions WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Transactions: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Settlements
UPDATE settlements 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM settlements WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Settlements: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Payments
UPDATE payments 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM payments WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Payments: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Notifications
UPDATE notifications 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM notifications WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Notifications: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Notification Queue
UPDATE notification_queue 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM notification_queue WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Notification Queue: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Announcements
UPDATE announcements 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM announcements WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Announcements: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Employee Records
UPDATE employee_records 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM employee_records WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Employee Records: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Merchant Admins
UPDATE merchant_admins 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM merchant_admins WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Merchant Admins: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Organization Admins
UPDATE organization_admins 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM organization_admins WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Organization Admins: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Repayment Schedules
UPDATE repayment_schedules 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM repayment_schedules WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Repayment Schedules: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Merchant Money Account Details
UPDATE merchant_money_account_details 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM merchant_money_account_details WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Merchant Money Account Details: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Payroll Deductions
UPDATE payroll_deductions 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM payroll_deductions WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Payroll Deductions: Updated ', ROW_COUNT(), ' records') AS Progress;

-- API Keys
UPDATE api_keys 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM api_keys WHERE uuid IS NULL) AS temp);

SELECT CONCAT('API Keys: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Roles
UPDATE role 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM role WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Roles: Updated ', ROW_COUNT(), ' records') AS Progress;

-- States
UPDATE states 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM states WHERE uuid IS NULL) AS temp);

SELECT CONCAT('States: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Countries
UPDATE countries 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM countries WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Countries: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Currencies
UPDATE currencies 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM currencies WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Currencies: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Client Balance
UPDATE client_balance 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM client_balance WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Client Balance: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Credit Score
UPDATE client_credit_score 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM client_credit_score WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Credit Score: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Employee Payslip Information
UPDATE employee_payslip_information 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM employee_payslip_information WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Employee Payslip Information: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Notification Requests
UPDATE notification_requests 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM notification_requests WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Notification Requests: Updated ', ROW_COUNT(), ' records') AS Progress;

-- OTPs
UPDATE otps 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM otps WHERE uuid IS NULL) AS temp);

SELECT CONCAT('OTPs: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Device Metadata
UPDATE device_meta_data 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM device_meta_data WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Device Metadata: Updated ', ROW_COUNT(), ' records') AS Progress;

-- Idempotency Records
UPDATE idempotency_records 
SET uuid = UUID() 
WHERE id IN (SELECT id FROM (SELECT id FROM idempotency_records WHERE uuid IS NULL) AS temp);

SELECT CONCAT('Idempotency Records: Updated ', ROW_COUNT(), ' records') AS Progress;

-- =============================================================================
-- Verify results
-- =============================================================================

SELECT 'Verification - records still without UUIDs:' AS Status;

SELECT 
    'users' as table_name,
    COUNT(*) as total_records,
    SUM(CASE WHEN uuid IS NULL THEN 1 ELSE 0 END) as still_null,
    SUM(CASE WHEN uuid IS NOT NULL THEN 1 ELSE 0 END) as has_uuid
FROM users
UNION ALL
SELECT 
    'merchants',
    COUNT(*),
    SUM(CASE WHEN uuid IS NULL THEN 1 ELSE 0 END),
    SUM(CASE WHEN uuid IS NOT NULL THEN 1 ELSE 0 END)
FROM merchants
UNION ALL
SELECT 
    'organizations',
    COUNT(*),
    SUM(CASE WHEN uuid IS NULL THEN 1 ELSE 0 END),
    SUM(CASE WHEN uuid IS NOT NULL THEN 1 ELSE 0 END)
FROM organizations
UNION ALL
SELECT 
    'clients',
    COUNT(*),
    SUM(CASE WHEN uuid IS NULL THEN 1 ELSE 0 END),
    SUM(CASE WHEN uuid IS NOT NULL THEN 1 ELSE 0 END)
FROM clients
UNION ALL
SELECT 
    'vouchers',
    COUNT(*),
    SUM(CASE WHEN uuid IS NULL THEN 1 ELSE 0 END),
    SUM(CASE WHEN uuid IS NOT NULL THEN 1 ELSE 0 END)
FROM vouchers;

-- Show sample UUIDs
SELECT 'Sample UUIDs generated:' AS Status;

SELECT id, uuid, email FROM users WHERE uuid IS NOT NULL LIMIT 3;
SELECT id, uuid, name FROM merchants WHERE uuid IS NOT NULL LIMIT 3;
SELECT id, uuid, name FROM organizations WHERE uuid IS NOT NULL LIMIT 3;

-- Re-enable safe mode
SET SQL_SAFE_UPDATES = 1;

SELECT 'UUID population complete! All existing records now have UUIDs.' AS Status;

