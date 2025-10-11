-- Diagnostic script to check which tables have UUID columns
-- Run this BEFORE the migration to see current state

SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_KEY,
    COLUMN_DEFAULT
FROM 
    INFORMATION_SCHEMA.COLUMNS
WHERE 
    TABLE_SCHEMA = DATABASE()
    AND COLUMN_NAME = 'uuid'
    AND TABLE_NAME IN (
        'users',
        'merchants',
        'organizations',
        'clients',
        'vouchers',
        'transactions',
        'settlements',
        'payments',
        'notifications',
        'notification_queue',
        'announcements',
        'employee_records',
        'merchant_admins',
        'organization_admins',
        'repayment_schedules',
        'merchant_money_account_details',
        'payroll_deductions',
        'api_keys',
        'role',
        'states',
        'countries',
        'currencies',
        'client_balance',
        'client_credit_score',
        'employee_payslip_information',
        'notification_requests',
        'otps',
        'device_meta_data',
        'idempotency_records'
    )
ORDER BY 
    TABLE_NAME;

-- Summary
SELECT 
    'Total tables with UUID column' AS Status,
    COUNT(*) AS Count
FROM 
    INFORMATION_SCHEMA.COLUMNS
WHERE 
    TABLE_SCHEMA = DATABASE()
    AND COLUMN_NAME = 'uuid'
    AND TABLE_NAME IN (
        'users', 'merchants', 'organizations', 'clients', 'vouchers',
        'transactions', 'settlements', 'payments', 'notifications',
        'notification_queue', 'announcements', 'employee_records',
        'merchant_admins', 'organization_admins', 'repayment_schedules',
        'merchant_money_account_details', 'payroll_deductions', 'api_keys',
        'role', 'states', 'countries', 'currencies', 'client_balance',
        'client_credit_score', 'employee_payslip_information',
        'notification_requests', 'otps', 'device_meta_data', 'idempotency_records'
    );

-- Check for NULL UUIDs in existing columns
SELECT 
    'users' AS table_name,
    COUNT(*) AS total_rows,
    SUM(CASE WHEN uuid IS NULL THEN 1 ELSE 0 END) AS null_uuids,
    SUM(CASE WHEN uuid IS NOT NULL THEN 1 ELSE 0 END) AS has_uuids
FROM users
WHERE (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'uuid') > 0

UNION ALL

SELECT 
    'merchants' AS table_name,
    COUNT(*) AS total_rows,
    SUM(CASE WHEN uuid IS NULL THEN 1 ELSE 0 END) AS null_uuids,
    SUM(CASE WHEN uuid IS NOT NULL THEN 1 ELSE 0 END) AS has_uuids
FROM merchants
WHERE (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchants' AND COLUMN_NAME = 'uuid') > 0

UNION ALL

SELECT 
    'organizations' AS table_name,
    COUNT(*) AS total_rows,
    SUM(CASE WHEN uuid IS NULL THEN 1 ELSE 0 END) AS null_uuids,
    SUM(CASE WHEN uuid IS NOT NULL THEN 1 ELSE 0 END) AS has_uuids
FROM organizations
WHERE (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'organizations' AND COLUMN_NAME = 'uuid') > 0

UNION ALL

SELECT 
    'clients' AS table_name,
    COUNT(*) AS total_rows,
    SUM(CASE WHEN uuid IS NULL THEN 1 ELSE 0 END) AS null_uuids,
    SUM(CASE WHEN uuid IS NOT NULL THEN 1 ELSE 0 END) AS has_uuids
FROM clients
WHERE (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'clients' AND COLUMN_NAME = 'uuid') > 0;

