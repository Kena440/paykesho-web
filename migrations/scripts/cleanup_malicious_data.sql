-- =====================================================
-- SECURITY: Clean Malicious Data from Database
-- =====================================================
-- This script identifies and removes malicious data that has been inserted
-- into the database through XSS attacks, SQL injection attempts, or spam.
--
-- IMPORTANT: Review the data before running DELETE operations
-- BACKUP your database before running this script!
-- =====================================================

-- Set safe update mode (require WHERE clause with key column)
SET SQL_SAFE_UPDATES = 1;

-- =====================================================
-- STEP 1: Identify Malicious Clients
-- =====================================================

-- Find clients with XSS patterns (script tags, javascript:, etc.)
SELECT 
    id, 
    name, 
    email, 
    address, 
    nrc,
    created_time,
    'XSS Pattern in Name' as issue_type
FROM paykesho_db.clients 
WHERE name REGEXP '<[^>]*>|javascript:|on[a-z]+=|<script|</script>'
    OR name LIKE '%<script%'
    OR name LIKE '%javascript:%'
    OR name LIKE '%onerror=%'
    OR name LIKE '%onclick=%';

-- Find clients with suspicious URLs in address field
SELECT 
    id, 
    name, 
    email, 
    address, 
    created_time,
    'URL in Address Field' as issue_type
FROM paykesho_db.clients 
WHERE address REGEXP 'https?://|www\.'
    OR address LIKE '%http://%'
    OR address LIKE '%https://%'
    OR address LIKE '%www.%';

-- Find clients with invalid NRC format (should be XXXXXX/XX/X)
SELECT 
    id, 
    name, 
    email, 
    nrc,
    created_time,
    'Invalid NRC Format' as issue_type
FROM paykesho_db.clients 
WHERE nrc NOT REGEXP '^[0-9]{6}/[0-9]{2}/[0-9]{1}$'
    AND nrc IS NOT NULL 
    AND nrc != '';

-- Find clients with HTML tags in any field
SELECT 
    id, 
    name, 
    email, 
    address,
    nrc,
    created_time,
    'HTML Tags Detected' as issue_type
FROM paykesho_db.clients 
WHERE name LIKE '%<%>%'
    OR email LIKE '%<%>%'
    OR address LIKE '%<%>%'
    OR nrc LIKE '%<%>%';

-- Find clients with suspicious email patterns
SELECT 
    id, 
    name, 
    email, 
    created_time,
    'Suspicious Email' as issue_type
FROM paykesho_db.clients 
WHERE email NOT REGEXP '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
    OR email LIKE '%<%'
    OR email LIKE '%>%'
    OR email LIKE '%script%';

-- =====================================================
-- STEP 2: Review Specific Malicious Records
-- =====================================================

-- Review the specific malicious records mentioned by user
SELECT * FROM paykesho_db.clients WHERE id IN (4, 6);

-- =====================================================
-- STEP 3: Delete Malicious Records (CAREFUL!)
-- =====================================================

-- Before deleting, make sure you've reviewed the records above
-- Uncomment the DELETE statements below ONLY after verification

-- Delete client with ID 4 (Spam/Viagra advertisement)
-- DELETE FROM paykesho_db.clients WHERE id = 4;

-- Delete client with ID 6 (XSS attack with script tag in name)
-- DELETE FROM paykesho_db.clients WHERE id = 6;

-- Alternative: Delete all clients with XSS patterns
-- WARNING: This is aggressive, review first!
/*
DELETE FROM paykesho_db.clients 
WHERE name REGEXP '<[^>]*>|javascript:|on[a-z]+=|<script|</script>'
    OR name LIKE '%<script%'
    OR name LIKE '%javascript:%'
    OR address REGEXP 'https?://'
    OR nrc REGEXP '<[^>]*>';
*/

-- =====================================================
-- STEP 4: Sanitize Existing Data
-- =====================================================

-- Create backup table first
CREATE TABLE IF NOT EXISTS paykesho_db.clients_backup_before_cleanup AS 
SELECT * FROM paykesho_db.clients;

-- Update records to remove HTML entities (use with caution)
-- This removes < and > characters
/*
UPDATE paykesho_db.clients 
SET 
    name = REPLACE(REPLACE(name, '<', ''), '>', ''),
    address = REPLACE(REPLACE(address, '<', ''), '>', ''),
    nrc = REPLACE(REPLACE(nrc, '<', ''), '>', '')
WHERE name LIKE '%<%' 
   OR name LIKE '%>%'
   OR address LIKE '%<%'
   OR address LIKE '%>%'
   OR nrc LIKE '%<%'
   OR nrc LIKE '%>%';
*/

-- =====================================================
-- STEP 5: Verify Cleanup
-- =====================================================

-- Count remaining clients with suspicious patterns
SELECT COUNT(*) as suspicious_clients_remaining
FROM paykesho_db.clients 
WHERE name REGEXP '<[^>]*>|javascript:|on[a-z]+='
    OR address REGEXP 'https?://'
    OR nrc REGEXP '<[^>]*>';

-- Show all remaining clients
SELECT id, name, email, address, nrc, created_time 
FROM paykesho_db.clients 
ORDER BY created_time DESC;

-- =====================================================
-- STEP 6: Clean Related Tables (if needed)
-- =====================================================

-- If you deleted clients, you may need to clean up related data
-- Check for orphaned records in client_balance table
/*
SELECT cb.* 
FROM paykesho_db.client_balance cb
LEFT JOIN paykesho_db.clients c ON cb.client_id = c.id
WHERE c.id IS NULL;
*/

-- Delete orphaned client balances
/*
DELETE FROM paykesho_db.client_balance 
WHERE client_id NOT IN (SELECT id FROM paykesho_db.clients);
*/

-- =====================================================
-- RECOMMENDED MANUAL STEPS:
-- =====================================================
-- 1. Review the SELECT queries above to identify malicious data
-- 2. Backup your database: mysqldump paykesho_db > backup.sql
-- 3. Uncomment and run the DELETE statements for confirmed malicious records
-- 4. Verify the cleanup with the verification queries
-- 5. Monitor logs for future attacks
-- 6. Ensure input validation is enabled in the application
-- =====================================================

-- Reset safe update mode
SET SQL_SAFE_UPDATES = 0;

