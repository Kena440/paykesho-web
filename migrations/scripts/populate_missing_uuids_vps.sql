-- =====================================================
-- POPULATE MISSING UUIDs ON VPS PRODUCTION
-- =====================================================
-- This script generates UUIDs for all records that have
-- NULL uuid values. This fixes the issue where links
-- like /merchants/view/{uuid} don't work because uuid is NULL
--
-- SAFE TO RUN MULTIPLE TIMES - only updates NULL uuids
-- =====================================================

USE paykesho_db;

SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO';

SELECT '======= CHECKING CURRENT UUID STATUS =======' AS status;

-- Check how many records have NULL uuids
SELECT 
    'merchants' as table_name,
    COUNT(*) as total_records,
    SUM(CASE WHEN uuid IS NULL THEN 1 ELSE 0 END) as null_uuids,
    SUM(CASE WHEN uuid IS NOT NULL THEN 1 ELSE 0 END) as has_uuids
FROM merchants

UNION ALL

SELECT 
    'clients' as table_name,
    COUNT(*) as total_records,
    SUM(CASE WHEN uuid IS NULL THEN 1 ELSE 0 END) as null_uuids,
    SUM(CASE WHEN uuid IS NOT NULL THEN 1 ELSE 0 END) as has_uuids
FROM clients

UNION ALL

SELECT 
    'organizations' as table_name,
    COUNT(*) as total_records,
    SUM(CASE WHEN uuid IS NULL THEN 1 ELSE 0 END) as null_uuids,
    SUM(CASE WHEN uuid IS NOT NULL THEN 1 ELSE 0 END) as has_uuids
FROM organizations

UNION ALL

SELECT 
    'users' as table_name,
    COUNT(*) as total_records,
    SUM(CASE WHEN uuid IS NULL THEN 1 ELSE 0 END) as null_uuids,
    SUM(CASE WHEN uuid IS NOT NULL THEN 1 ELSE 0 END) as has_uuids
FROM users;

SELECT '======= POPULATING MISSING UUIDs =======' AS status;

-- =====================================================
-- POPULATE UUIDs FOR MERCHANTS
-- =====================================================

UPDATE merchants 
SET uuid = UUID()
WHERE uuid IS NULL OR uuid = '';

SELECT CONCAT('Merchants: Updated ', ROW_COUNT(), ' records with UUIDs') AS result;

-- =====================================================
-- POPULATE UUIDs FOR CLIENTS
-- =====================================================

UPDATE clients 
SET uuid = UUID()
WHERE uuid IS NULL OR uuid = '';

SELECT CONCAT('Clients: Updated ', ROW_COUNT(), ' records with UUIDs') AS result;

-- =====================================================
-- POPULATE UUIDs FOR ORGANIZATIONS
-- =====================================================

UPDATE organizations 
SET uuid = UUID()
WHERE uuid IS NULL OR uuid = '';

SELECT CONCAT('Organizations: Updated ', ROW_COUNT(), ' records with UUIDs') AS result;

-- =====================================================
-- POPULATE UUIDs FOR USERS
-- =====================================================

UPDATE users 
SET uuid = UUID()
WHERE uuid IS NULL OR uuid = '';

SELECT CONCAT('Users: Updated ', ROW_COUNT(), ' records with UUIDs') AS result;

-- =====================================================
-- POPULATE UUIDs FOR MERCHANT_ADMINS
-- =====================================================

UPDATE merchant_admins 
SET uuid = UUID()
WHERE uuid IS NULL OR uuid = '';

SELECT CONCAT('Merchant Admins: Updated ', ROW_COUNT(), ' records with UUIDs') AS result;

-- =====================================================
-- POPULATE UUIDs FOR ORGANIZATION_ADMINS
-- =====================================================

UPDATE organization_admins 
SET uuid = UUID()
WHERE uuid IS NULL OR uuid = '';

SELECT CONCAT('Organization Admins: Updated ', ROW_COUNT(), ' records with UUIDs') AS result;

-- =====================================================
-- POPULATE UUIDs FOR ANNOUNCEMENTS
-- =====================================================

UPDATE announcements 
SET uuid = UUID()
WHERE uuid IS NULL OR uuid = '';

SELECT CONCAT('Announcements: Updated ', ROW_COUNT(), ' records with UUIDs') AS result;

-- =====================================================
-- POPULATE UUIDs FOR VOUCHERS
-- =====================================================

UPDATE vouchers 
SET uuid = UUID()
WHERE uuid IS NULL OR uuid = '';

SELECT CONCAT('Vouchers: Updated ', ROW_COUNT(), ' records with UUIDs') AS result;

-- =====================================================
-- POPULATE UUIDs FOR EMPLOYEE_RECORDS
-- =====================================================

UPDATE employee_records 
SET uuid = UUID()
WHERE uuid IS NULL OR uuid = '';

SELECT CONCAT('Employee Records: Updated ', ROW_COUNT(), ' records with UUIDs') AS result;

-- =====================================================
-- VERIFY ALL UUIDs POPULATED
-- =====================================================

SELECT '======= VERIFICATION =======' AS status;

SELECT 
    'merchants' as table_name,
    COUNT(*) as total_records,
    SUM(CASE WHEN uuid IS NULL OR uuid = '' THEN 1 ELSE 0 END) as null_uuids,
    SUM(CASE WHEN uuid IS NOT NULL AND uuid != '' THEN 1 ELSE 0 END) as has_uuids,
    CASE 
        WHEN SUM(CASE WHEN uuid IS NULL OR uuid = '' THEN 1 ELSE 0 END) = 0 
        THEN '✅ ALL HAVE UUIDs'
        ELSE '❌ STILL MISSING UUIDs'
    END as status
FROM merchants

UNION ALL

SELECT 
    'clients',
    COUNT(*),
    SUM(CASE WHEN uuid IS NULL OR uuid = '' THEN 1 ELSE 0 END),
    SUM(CASE WHEN uuid IS NOT NULL AND uuid != '' THEN 1 ELSE 0 END),
    CASE WHEN SUM(CASE WHEN uuid IS NULL OR uuid = '' THEN 1 ELSE 0 END) = 0 THEN '✅ ALL HAVE UUIDs' ELSE '❌ STILL MISSING UUIDs' END
FROM clients

UNION ALL

SELECT 
    'organizations',
    COUNT(*),
    SUM(CASE WHEN uuid IS NULL OR uuid = '' THEN 1 ELSE 0 END),
    SUM(CASE WHEN uuid IS NOT NULL AND uuid != '' THEN 1 ELSE 0 END),
    CASE WHEN SUM(CASE WHEN uuid IS NULL OR uuid = '' THEN 1 ELSE 0 END) = 0 THEN '✅ ALL HAVE UUIDs' ELSE '❌ STILL MISSING UUIDs' END
FROM organizations

UNION ALL

SELECT 
    'users',
    COUNT(*),
    SUM(CASE WHEN uuid IS NULL OR uuid = '' THEN 1 ELSE 0 END),
    SUM(CASE WHEN uuid IS NOT NULL AND uuid != '' THEN 1 ELSE 0 END),
    CASE WHEN SUM(CASE WHEN uuid IS NULL OR uuid = '' THEN 1 ELSE 0 END) = 0 THEN '✅ ALL HAVE UUIDs' ELSE '❌ STILL MISSING UUIDs' END
FROM users;

-- Show sample UUIDs
SELECT '======= SAMPLE UUIDs =======' AS status;

SELECT 'merchants' as table_name, id, uuid 
FROM merchants 
WHERE uuid IS NOT NULL 
LIMIT 5;

SELECT '======= UUID POPULATION COMPLETE =======' AS status;
SELECT 'All records now have UUIDs!' AS message;
SELECT 'Links like /merchants/view/{uuid} will now work!' AS next_step;

SET SQL_MODE=@OLD_SQL_MODE;

