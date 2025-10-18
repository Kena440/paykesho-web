-- =====================================================
-- COMPREHENSIVE SECURITY: Clean All Malicious Data
-- =====================================================
-- This script scans ALL user-facing entities for malicious content
-- including XSS attacks, SQL injection, spam, and invalid data
--
-- IMPORTANT: BACKUP DATABASE BEFORE RUNNING!
-- Run: mysqldump -u root -p paykesho_db > backup_$(date +%Y%m%d).sql
-- =====================================================

SET SQL_SAFE_UPDATES = 1;

-- =====================================================
-- SECTION 1: CLIENTS (ALREADY ADDRESSED)
-- =====================================================

SELECT '======= SCANNING CLIENTS TABLE =======' AS status;

SELECT id, name, email, address, nrc, 'XSS/Malicious' as issue_type
FROM paykesho_db.clients 
WHERE name REGEXP '<[^>]*>|javascript:|on[a-z]+=|<script'
   OR address REGEXP 'https?://'
   OR nrc NOT REGEXP '^[0-9]{6}/[0-9]{2}/[0-9]{1}$'
   OR email LIKE '%<%' OR email LIKE '%>%'
LIMIT 50;

-- =====================================================
-- SECTION 2: ANNOUNCEMENTS (CRITICAL - PUBLIC)
-- =====================================================

SELECT '======= SCANNING ANNOUNCEMENTS TABLE =======' AS status;

SELECT id, name, details, event_date, 'XSS/Malicious' as issue_type
FROM paykesho_db.announcements
WHERE name REGEXP '<[^>]*>|javascript:|on[a-z]+=|<script'
   OR details REGEXP '<[^>]*>|javascript:|on[a-z]+=|<script'
   OR name LIKE '%<%' OR name LIKE '%>%'
   OR details LIKE '%<%' OR details LIKE '%>%'
LIMIT 50;

-- Delete malicious announcements (uncomment after review)
/*
DELETE FROM paykesho_db.announcements
WHERE name REGEXP '<[^>]*>|javascript:|on[a-z]+=|<script'
   OR details REGEXP '<[^>]*>|javascript:|on[a-z]+=|<script';
*/

-- =====================================================
-- SECTION 3: NOTIFICATIONS (CRITICAL - USER MESSAGES)
-- =====================================================

SELECT '======= SCANNING NOTIFICATIONS TABLE =======' AS status;

SELECT id, reference, recipient, message, status, 'XSS/Malicious' as issue_type
FROM paykesho_db.notifications
WHERE message REGEXP '<[^>]*>|javascript:|on[a-z]+=|<script'
   OR recipient REGEXP '<[^>]*>|<script'
   OR reference REGEXP '<[^>]*>|<script'
LIMIT 50;

-- Delete malicious notifications (uncomment after review)
/*
DELETE FROM paykesho_db.notifications
WHERE message REGEXP '<[^>]*>|javascript:|on[a-z]+=|<script';
*/

-- =====================================================
-- SECTION 3B: NOTIFICATION QUEUE (CRITICAL - QUEUED MESSAGES)
-- =====================================================

SELECT '======= SCANNING NOTIFICATION_QUEUE TABLE =======' AS status;

-- Check if table exists first
SELECT IF(
    EXISTS(
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = 'paykesho_db' 
        AND table_name = 'notification_queue'
    ),
    'Table exists - scanning...',
    'Table does not exist - will be auto-created on restart'
) AS table_status;

-- Scan only if table exists
SELECT id, recipient_name, subject, 
       SUBSTRING(content, 1, 100) as content_preview, 
       'XSS/Malicious' as issue_type
FROM paykesho_db.notification_queue
WHERE (recipient_name REGEXP '<[^>]*>|javascript:|<script' AND recipient_name IS NOT NULL)
   OR (subject REGEXP '<[^>]*>|javascript:|<script' AND subject IS NOT NULL)
   OR (content REGEXP '<[^>]*>|javascript:|<script' AND content IS NOT NULL)
LIMIT 50;

-- Delete malicious queued notifications (uncomment after review)
/*
DELETE FROM paykesho_db.notification_queue
WHERE (recipient_name REGEXP '<[^>]*>|<script' AND recipient_name IS NOT NULL)
   OR (subject REGEXP '<[^>]*>|<script' AND subject IS NOT NULL)
   OR (content REGEXP '<[^>]*>|<script' AND content IS NOT NULL);
*/

-- =====================================================
-- SECTION 4: FILE UPLOADS (CRITICAL - PATH TRAVERSAL)
-- =====================================================

SELECT '======= SCANNING FILE_UPLOADS TABLE =======' AS status;

-- Check if description column exists, if not, query without it
SELECT id, file_name, file_path, 
       CASE 
           WHEN file_name LIKE '%..%' THEN 'Path Traversal in filename'
           WHEN file_path LIKE '%..%' THEN 'Path Traversal in path'
           WHEN file_name REGEXP '<[^>]*>|javascript:|<script' THEN 'XSS in filename'
           WHEN file_name LIKE '%/%' OR file_name LIKE '%\\%' THEN 'Invalid path separators'
           ELSE 'Malicious pattern detected'
       END as issue_type
FROM paykesho_db.file_uploads
WHERE file_name LIKE '%..%' 
   OR file_path LIKE '%..%'
   OR file_name REGEXP '<[^>]*>|javascript:|<script'
   OR file_name LIKE '%/%'
   OR file_name LIKE '%\\%'
LIMIT 50;

-- Delete malicious file uploads (uncomment after review)
/*
DELETE FROM paykesho_db.file_uploads
WHERE file_name LIKE '%..%' 
   OR file_path LIKE '%..%'
   OR file_name REGEXP '<[^>]*>|<script';
*/

-- =====================================================
-- SECTION 5: USERS (ADMIN ACCOUNTS)
-- =====================================================

SELECT '======= SCANNING USERS TABLE =======' AS status;

SELECT id, email, first_name, last_name, 'XSS/Malicious' as issue_type
FROM paykesho_db.users
WHERE first_name REGEXP '<[^>]*>|javascript:|on[a-z]+=|<script'
   OR last_name REGEXP '<[^>]*>|javascript:|on[a-z]+=|<script'
   OR email REGEXP '<[^>]*>|<script'
   OR first_name LIKE '%<%' OR last_name LIKE '%>%'
LIMIT 50;

-- Delete malicious users (uncomment after review)
/*
DELETE FROM paykesho_db.users
WHERE first_name REGEXP '<[^>]*>|<script'
   OR last_name REGEXP '<[^>]*>|<script';
*/

-- =====================================================
-- SECTION 6: ORGANIZATION ADMINS
-- =====================================================

SELECT '======= SCANNING ORGANIZATION_ADMINS TABLE =======' AS status;

SELECT id, name, title, email, phone_number, 'XSS/Malicious' as issue_type
FROM paykesho_db.organization_admins
WHERE name REGEXP '<[^>]*>|javascript:|on[a-z]+=|<script'
   OR title REGEXP '<[^>]*>|<script'
   OR email REGEXP '<[^>]*>|<script'
   OR name LIKE '%<%' OR name LIKE '%>%'
LIMIT 50;

-- Delete malicious organization admins (uncomment after review)
/*
DELETE FROM paykesho_db.organization_admins
WHERE name REGEXP '<[^>]*>|<script'
   OR title REGEXP '<[^>]*>|<script';
*/

-- =====================================================
-- SECTION 7: MERCHANT ADMINS
-- =====================================================

SELECT '======= SCANNING MERCHANT_ADMINS TABLE =======' AS status;

SELECT id, name, title, email, phone_number, 'XSS/Malicious' as issue_type
FROM paykesho_db.merchant_admins
WHERE name REGEXP '<[^>]*>|javascript:|on[a-z]+=|<script'
   OR title REGEXP '<[^>]*>|<script'
   OR email REGEXP '<[^>]*>|<script'
   OR name LIKE '%<%' OR name LIKE '%>%'
LIMIT 50;

-- Delete malicious merchant admins (uncomment after review)
/*
DELETE FROM paykesho_db.merchant_admins
WHERE name REGEXP '<[^>]*>|<script'
   OR title REGEXP '<[^>]*>|<script';
*/

-- =====================================================
-- SECTION 8: ORGANIZATIONS
-- =====================================================

SELECT '======= SCANNING ORGANIZATIONS TABLE =======' AS status;

SELECT id, name, email, phone_number, 'XSS/Malicious' as issue_type
FROM paykesho_db.organizations
WHERE name REGEXP '<[^>]*>|javascript:|on[a-z]+=|<script'
   OR email REGEXP '<[^>]*>|<script'
   OR name LIKE '%<%' OR name LIKE '%>%'
LIMIT 50;

-- Delete malicious organizations (uncomment after review)
/*
DELETE FROM paykesho_db.organizations
WHERE name REGEXP '<[^>]*>|<script';
*/

-- =====================================================
-- SECTION 9: MERCHANTS
-- =====================================================

SELECT '======= SCANNING MERCHANTS TABLE =======' AS status;

SELECT id, name, details, email, physical_address, services_offered, 'XSS/Malicious' as issue_type
FROM paykesho_db.merchants
WHERE name REGEXP '<[^>]*>|javascript:|on[a-z]+=|<script'
   OR details REGEXP '<[^>]*>|<script'
   OR email REGEXP '<[^>]*>|<script'
   OR physical_address REGEXP 'https?://'
   OR services_offered REGEXP '<[^>]*>|<script'
   OR name LIKE '%<%' OR details LIKE '%>%'
LIMIT 50;

-- Delete malicious merchants (uncomment after review)
/*
DELETE FROM paykesho_db.merchants
WHERE name REGEXP '<[^>]*>|<script'
   OR details REGEXP '<[^>]*>|<script'
   OR physical_address REGEXP 'https?://';
*/

-- =====================================================
-- SECTION 10: EMPLOYEE RECORDS
-- =====================================================

SELECT '======= SCANNING EMPLOYEE_RECORDS TABLE =======' AS status;

SELECT id, name, nrc, tpin, man_number, 'XSS/Invalid Format' as issue_type
FROM paykesho_db.employee_records
WHERE name REGEXP '<[^>]*>|javascript:|on[a-z]+=|<script'
   OR nrc NOT REGEXP '^[0-9]{6}/[0-9]{2}/[0-9]{1}$'
   OR tpin NOT REGEXP '^[0-9]{10}$'
   OR man_number NOT REGEXP '^[0-9]{1,10}$'
   OR name LIKE '%<%'
LIMIT 50;

-- Delete malicious employee records (uncomment after review)
/*
DELETE FROM paykesho_db.employee_records
WHERE name REGEXP '<[^>]*>|<script'
   OR nrc NOT REGEXP '^[0-9]{6}/[0-9]{2}/[0-9]{1}$';
*/

-- =====================================================
-- SECTION 11: SUMMARY STATISTICS
-- =====================================================

SELECT '======= SECURITY SCAN SUMMARY =======' AS status;

SELECT 
    'Clients' as table_name,
    COUNT(*) as suspicious_records
FROM paykesho_db.clients 
WHERE name REGEXP '<[^>]*>|javascript:|on[a-z]+='
   OR address REGEXP 'https?://'
   OR nrc NOT REGEXP '^[0-9]{6}/[0-9]{2}/[0-9]{1}$'

UNION ALL

SELECT 
    'Announcements' as table_name,
    COUNT(*) as suspicious_records
FROM paykesho_db.announcements
WHERE name REGEXP '<[^>]*>|javascript:|<script'
   OR details REGEXP '<[^>]*>|javascript:|<script'

UNION ALL

SELECT 
    'Notifications' as table_name,
    COUNT(*) as suspicious_records
FROM paykesho_db.notifications
WHERE message REGEXP '<[^>]*>|javascript:|<script'

UNION ALL

SELECT 
    'Notification Queue' as table_name,
    COALESCE(
        (SELECT COUNT(*) FROM paykesho_db.notification_queue
         WHERE (recipient_name REGEXP '<[^>]*>|<script' AND recipient_name IS NOT NULL)
            OR (subject REGEXP '<[^>]*>|<script' AND subject IS NOT NULL)
            OR (content REGEXP '<[^>]*>|<script' AND content IS NOT NULL)),
        0
    ) as suspicious_records
    
UNION ALL

SELECT 
    'File Uploads' as table_name,
    COUNT(*) as suspicious_records
FROM paykesho_db.file_uploads
WHERE file_name LIKE '%..%' 
   OR file_path LIKE '%..%'
   OR file_name REGEXP '<[^>]*>|<script'

UNION ALL

SELECT 
    'Users' as table_name,
    COUNT(*) as suspicious_records
FROM paykesho_db.users
WHERE first_name REGEXP '<[^>]*>|<script'
   OR last_name REGEXP '<[^>]*>|<script'

UNION ALL

SELECT 
    'Organization Admins' as table_name,
    COUNT(*) as suspicious_records
FROM paykesho_db.organization_admins
WHERE name REGEXP '<[^>]*>|<script'

UNION ALL

SELECT 
    'Merchant Admins' as table_name,
    COUNT(*) as suspicious_records
FROM paykesho_db.merchant_admins
WHERE name REGEXP '<[^>]*>|<script'

UNION ALL

SELECT 
    'Organizations' as table_name,
    COUNT(*) as suspicious_records
FROM paykesho_db.organizations
WHERE name REGEXP '<[^>]*>|<script'

UNION ALL

SELECT 
    'Merchants' as table_name,
    COUNT(*) as suspicious_records
FROM paykesho_db.merchants
WHERE name REGEXP '<[^>]*>|<script'
   OR details REGEXP '<[^>]*>|<script'
   OR physical_address REGEXP 'https?://'

UNION ALL

SELECT 
    'Employee Records' as table_name,
    COUNT(*) as suspicious_records
FROM paykesho_db.employee_records
WHERE name REGEXP '<[^>]*>|<script'
   OR nrc NOT REGEXP '^[0-9]{6}/[0-9]{2}/[0-9]{1}$';

-- =====================================================
-- RECOMMENDED ACTIONS:
-- =====================================================
-- 1. Review each section's SELECT results
-- 2. Backup database: mysqldump paykesho_db > backup.sql
-- 3. Uncomment DELETE statements for confirmed malicious data
-- 4. Re-run this script to verify cleanup
-- 5. Monitor logs for new attacks
-- 6. Ensure application security measures are active
-- =====================================================

SET SQL_SAFE_UPDATES = 0;

SELECT '======= SCAN COMPLETE =======' AS status;
SELECT 'Review results above and uncomment DELETE statements if malicious data found' AS next_action;

