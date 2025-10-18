-- =====================================================
-- VERIFY ALL TABLES EXIST ON VPS
-- =====================================================
-- This script checks if all required tables exist
-- Shows which tables are missing
-- =====================================================

USE paykesho_db;

SELECT '======= TABLE EXISTENCE CHECK =======' AS status;

-- List of all expected tables
SELECT 
    'clients' as table_name,
    CASE 
        WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = 'clients')
        THEN '✓ EXISTS'
        ELSE '✗ MISSING'
    END AS status
    
UNION ALL SELECT 'users', CASE WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = 'users') THEN '✓ EXISTS' ELSE '✗ MISSING' END
UNION ALL SELECT 'announcements', CASE WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = 'announcements') THEN '✓ EXISTS' ELSE '✗ MISSING' END
UNION ALL SELECT 'notifications', CASE WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = 'notifications') THEN '✓ EXISTS' ELSE '✗ MISSING' END
UNION ALL SELECT 'notification_queue', CASE WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = 'notification_queue') THEN '✓ EXISTS' ELSE '✗ MISSING' END
UNION ALL SELECT 'file_uploads', CASE WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = 'file_uploads') THEN '✓ EXISTS' ELSE '✗ MISSING' END
UNION ALL SELECT 'security_audits', CASE WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = 'security_audits') THEN '✓ EXISTS' ELSE '✗ MISSING' END
UNION ALL SELECT 'organizations', CASE WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = 'organizations') THEN '✓ EXISTS' ELSE '✗ MISSING' END
UNION ALL SELECT 'organization_admins', CASE WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = 'organization_admins') THEN '✓ EXISTS' ELSE '✗ MISSING' END
UNION ALL SELECT 'merchants', CASE WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = 'merchants') THEN '✓ EXISTS' ELSE '✗ MISSING' END
UNION ALL SELECT 'merchant_admins', CASE WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = 'merchant_admins') THEN '✓ EXISTS' ELSE '✗ MISSING' END
UNION ALL SELECT 'employee_records', CASE WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = 'employee_records') THEN '✓ EXISTS' ELSE '✗ MISSING' END
UNION ALL SELECT 'client_balance', CASE WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = 'client_balance') THEN '✓ EXISTS' ELSE '✗ MISSING' END
UNION ALL SELECT 'transactions', CASE WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = 'transactions') THEN '✓ EXISTS' ELSE '✗ MISSING' END
UNION ALL SELECT 'payments', CASE WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = 'payments') THEN '✓ EXISTS' ELSE '✗ MISSING' END
UNION ALL SELECT 'vouchers', CASE WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = 'vouchers') THEN '✓ EXISTS' ELSE '✗ MISSING' END
UNION ALL SELECT 'settlements', CASE WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = 'settlements') THEN '✓ EXISTS' ELSE '✗ MISSING' END

ORDER BY 
    CASE WHEN status = '✗ MISSING' THEN 0 ELSE 1 END,
    table_name;

-- Show summary
SELECT 
    COUNT(*) as total_expected_tables,
    SUM(CASE WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = t.table_name) THEN 1 ELSE 0 END) as tables_exist,
    COUNT(*) - SUM(CASE WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = t.table_name) THEN 1 ELSE 0 END) as tables_missing
FROM (
    SELECT 'clients' as table_name
    UNION ALL SELECT 'users'
    UNION ALL SELECT 'security_audits'
    UNION ALL SELECT 'notification_queue'
    UNION ALL SELECT 'announcements'
    UNION ALL SELECT 'notifications'
    UNION ALL SELECT 'file_uploads'
    UNION ALL SELECT 'organizations'
    UNION ALL SELECT 'merchants'
    UNION ALL SELECT 'employee_records'
) t;

SELECT '======= CHECK COMPLETE =======' AS status;

