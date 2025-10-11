-- Simple script to populate UUIDs for existing data
-- Works with safe mode by using primary key in WHERE clause

-- Disable safe mode
SET SQL_SAFE_UPDATES = 0;

-- =============================================================================
-- Populate UUIDs - Simple approach with primary key
-- =============================================================================

-- Users
UPDATE users SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Users updated: ', ROW_COUNT()) AS Result;

-- Merchants  
UPDATE merchants SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Merchants updated: ', ROW_COUNT()) AS Result;

-- Organizations
UPDATE organizations SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Organizations updated: ', ROW_COUNT()) AS Result;

-- Clients
UPDATE clients SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Clients updated: ', ROW_COUNT()) AS Result;

-- Vouchers
UPDATE vouchers SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Vouchers updated: ', ROW_COUNT()) AS Result;

-- Transactions
UPDATE transactions SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Transactions updated: ', ROW_COUNT()) AS Result;

-- Settlements
UPDATE settlements SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Settlements updated: ', ROW_COUNT()) AS Result;

-- Payments
UPDATE payments SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Payments updated: ', ROW_COUNT()) AS Result;

-- Notifications
UPDATE notifications SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Notifications updated: ', ROW_COUNT()) AS Result;

-- Notification Queue
UPDATE notification_queue SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Notification Queue updated: ', ROW_COUNT()) AS Result;

-- Announcements
UPDATE announcements SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Announcements updated: ', ROW_COUNT()) AS Result;

-- Employee Records
UPDATE employee_records SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Employee Records updated: ', ROW_COUNT()) AS Result;

-- Merchant Admins
UPDATE merchant_admins SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Merchant Admins updated: ', ROW_COUNT()) AS Result;

-- Organization Admins
UPDATE organization_admins SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Organization Admins updated: ', ROW_COUNT()) AS Result;

-- Repayment Schedules
UPDATE repayment_schedules SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Repayment Schedules updated: ', ROW_COUNT()) AS Result;

-- Merchant Money Account Details
UPDATE merchant_money_account_details SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Merchant Money Account Details updated: ', ROW_COUNT()) AS Result;

-- Payroll Deductions
UPDATE payroll_deductions SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Payroll Deductions updated: ', ROW_COUNT()) AS Result;

-- API Keys
UPDATE api_keys SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('API Keys updated: ', ROW_COUNT()) AS Result;

-- Roles
UPDATE role SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Roles updated: ', ROW_COUNT()) AS Result;

-- States
UPDATE states SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('States updated: ', ROW_COUNT()) AS Result;

-- Countries
UPDATE countries SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Countries updated: ', ROW_COUNT()) AS Result;

-- Currencies
UPDATE currencies SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Currencies updated: ', ROW_COUNT()) AS Result;

-- Client Balance
UPDATE client_balance SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Client Balance updated: ', ROW_COUNT()) AS Result;

-- Credit Score
UPDATE client_credit_score SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Credit Score updated: ', ROW_COUNT()) AS Result;

-- Employee Payslip Information
UPDATE employee_payslip_information SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Employee Payslip Information updated: ', ROW_COUNT()) AS Result;

-- Notification Requests
UPDATE notification_requests SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Notification Requests updated: ', ROW_COUNT()) AS Result;

-- OTPs
UPDATE otps SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('OTPs updated: ', ROW_COUNT()) AS Result;

-- Device Metadata
UPDATE device_meta_data SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Device Metadata updated: ', ROW_COUNT()) AS Result;

-- Idempotency Records
UPDATE idempotency_records SET uuid = UUID() WHERE id >= 1 AND (uuid IS NULL OR uuid = '');
SELECT CONCAT('Idempotency Records updated: ', ROW_COUNT()) AS Result;

-- =============================================================================
-- VERIFY - Show sample results
-- =============================================================================

SELECT '=== VERIFICATION: Sample UUIDs Generated ===' AS Status;

-- Show sample from users
SELECT 'USERS TABLE SAMPLE:' AS Info;
SELECT id, uuid, email FROM users LIMIT 5;

-- Show sample from merchants
SELECT 'MERCHANTS TABLE SAMPLE:' AS Info;
SELECT id, uuid, name FROM merchants LIMIT 5;

-- Show sample from clients
SELECT 'CLIENTS TABLE SAMPLE:' AS Info;
SELECT id, uuid, name FROM clients LIMIT 5;

-- Count any remaining NULL UUIDs
SELECT '=== FINAL CHECK: Any NULL UUIDs Remaining? ===' AS Status;

SELECT 
    'users' as table_name,
    COUNT(*) as total,
    SUM(CASE WHEN uuid IS NULL THEN 1 ELSE 0 END) as still_null
FROM users
UNION ALL
SELECT 
    'merchants',
    COUNT(*),
    SUM(CASE WHEN uuid IS NULL THEN 1 ELSE 0 END)
FROM merchants
UNION ALL
SELECT 
    'clients',
    COUNT(*),
    SUM(CASE WHEN uuid IS NULL THEN 1 ELSE 0 END)
FROM clients
UNION ALL
SELECT 
    'vouchers',
    COUNT(*),
    SUM(CASE WHEN uuid IS NULL THEN 1 ELSE 0 END)
FROM vouchers;

-- Re-enable safe mode
SET SQL_SAFE_UPDATES = 1;

SELECT 'COMPLETE! All existing records now have UUIDs.' AS FinalStatus;

