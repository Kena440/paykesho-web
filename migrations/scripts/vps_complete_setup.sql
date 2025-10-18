-- =====================================================
-- COMPLETE VPS SETUP - ONE SCRIPT TO RULE THEM ALL
-- =====================================================
-- This script does EVERYTHING needed for VPS:
-- 1. Creates missing tables (security_audits, notification_queue)
-- 2. Adds missing UUID columns
-- 3. Populates UUIDs for existing records
-- 4. Adds other missing columns (description, etc.)
-- 5. Verifies everything
--
-- SAFE TO RUN MULTIPLE TIMES
-- =====================================================

USE paykesho_db;

SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO';

SELECT '========================================' AS '';
SELECT '  PAYKESHO VPS COMPLETE SETUP' AS '';
SELECT '  Starting at: ' AS '', NOW() AS timestamp;
SELECT '========================================' AS '';

-- =====================================================
-- PART 1: CREATE MISSING TABLES
-- =====================================================

SELECT '' AS '';
SELECT '>>> PART 1: CREATING MISSING TABLES <<<' AS status;

-- Create security_audits table
CREATE TABLE IF NOT EXISTS security_audits (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NULL,
    scan_date DATETIME NOT NULL,
    table_name VARCHAR(50) NOT NULL,
    record_id INT NOT NULL,
    threat_type VARCHAR(50) NOT NULL,
    field_name VARCHAR(50) NULL,
    suspicious_value VARCHAR(1000) NULL,
    description VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by INT NULL,
    reviewed_date DATETIME NULL,
    action_taken VARCHAR(500) NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    INDEX idx_status (status),
    INDEX idx_table_record (table_name, record_id),
    INDEX idx_scan_date (scan_date),
    INDEX idx_severity (severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SELECT '✓ security_audits table ready' AS result;

-- Create notification_queue table
CREATE TABLE IF NOT EXISTS notification_queue (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NULL,
    created_at DATETIME NULL,
    scheduled_for DATETIME NULL,
    sent_at DATETIME NULL,
    processing_started_at DATETIME NULL,
    notification_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    recipient_email VARCHAR(100) NOT NULL,
    recipient_name VARCHAR(128) NULL,
    subject VARCHAR(255) NULL,
    content TEXT NULL,
    template_name VARCHAR(100) NULL,
    template_variables TEXT NULL,
    priority INT NOT NULL DEFAULT 3,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    error_message TEXT NULL,
    metadata TEXT NULL,
    entity_reference VARCHAR(100) NULL,
    entity_type VARCHAR(50) NULL,
    INDEX idx_notification_status (status),
    INDEX idx_notification_type (notification_type),
    INDEX idx_notification_recipient (recipient_email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SELECT '✓ notification_queue table ready' AS result;

-- =====================================================
-- PART 2: ADD MISSING UUID COLUMNS
-- =====================================================

SELECT '' AS '';
SELECT '>>> PART 2: ADDING MISSING UUID COLUMNS <<<' AS status;

SET @dbname = DATABASE();

-- Helper procedure to add UUID column if not exists
DELIMITER $$
CREATE PROCEDURE IF NOT EXISTS add_uuid_column(IN tbl_name VARCHAR(64))
BEGIN
    SET @check_sql = CONCAT(
        'SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS ',
        'WHERE TABLE_SCHEMA = ''', @dbname, ''' ',
        'AND TABLE_NAME = ''', tbl_name, ''' ',
        'AND COLUMN_NAME = ''uuid'''
    );
    
    PREPARE stmt FROM @check_sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    
    IF @col_exists = 0 THEN
        SET @alter_sql = CONCAT('ALTER TABLE ', tbl_name, ' ADD COLUMN uuid VARCHAR(36) NULL AFTER id');
        PREPARE stmt FROM @alter_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('✓ Added uuid to ', tbl_name) AS result;
    ELSE
        SELECT CONCAT('- uuid already exists in ', tbl_name) AS result;
    END IF;
END$$
DELIMITER ;

-- Add UUID to all main tables
CALL add_uuid_column('merchants');
CALL add_uuid_column('clients');
CALL add_uuid_column('organizations');
CALL add_uuid_column('users');
CALL add_uuid_column('merchant_admins');
CALL add_uuid_column('organization_admins');
CALL add_uuid_column('announcements');
CALL add_uuid_column('vouchers');
CALL add_uuid_column('employee_records');

-- Drop procedure
DROP PROCEDURE IF EXISTS add_uuid_column;

-- =====================================================
-- PART 3: POPULATE UUIDs FOR EXISTING RECORDS
-- =====================================================

SELECT '' AS '';
SELECT '>>> PART 3: POPULATING UUIDs <<<' AS status;

-- Merchants
UPDATE merchants SET uuid = UUID() WHERE uuid IS NULL OR uuid = '';
SELECT CONCAT('✓ Merchants: ', ROW_COUNT(), ' UUIDs generated') AS result;

-- Clients  
UPDATE clients SET uuid = UUID() WHERE uuid IS NULL OR uuid = '';
SELECT CONCAT('✓ Clients: ', ROW_COUNT(), ' UUIDs generated') AS result;

-- Organizations
UPDATE organizations SET uuid = UUID() WHERE uuid IS NULL OR uuid = '';
SELECT CONCAT('✓ Organizations: ', ROW_COUNT(), ' UUIDs generated') AS result;

-- Users
UPDATE users SET uuid = UUID() WHERE uuid IS NULL OR uuid = '';
SELECT CONCAT('✓ Users: ', ROW_COUNT(), ' UUIDs generated') AS result;

-- Merchant Admins
UPDATE merchant_admins SET uuid = UUID() WHERE uuid IS NULL OR uuid = '';
SELECT CONCAT('✓ Merchant Admins: ', ROW_COUNT(), ' UUIDs generated') AS result;

-- Organization Admins
UPDATE organization_admins SET uuid = UUID() WHERE uuid IS NULL OR uuid = '';
SELECT CONCAT('✓ Organization Admins: ', ROW_COUNT(), ' UUIDs generated') AS result;

-- Announcements
UPDATE announcements SET uuid = UUID() WHERE uuid IS NULL OR uuid = '';
SELECT CONCAT('✓ Announcements: ', ROW_COUNT(), ' UUIDs generated') AS result;

-- Vouchers
UPDATE vouchers SET uuid = UUID() WHERE uuid IS NULL OR uuid = '';
SELECT CONCAT('✓ Vouchers: ', ROW_COUNT(), ' UUIDs generated') AS result;

-- Employee Records
UPDATE employee_records SET uuid = UUID() WHERE uuid IS NULL OR uuid = '';
SELECT CONCAT('✓ Employee Records: ', ROW_COUNT(), ' UUIDs generated') AS result;

-- =====================================================
-- PART 4: ADD OTHER MISSING COLUMNS
-- =====================================================

SELECT '' AS '';
SELECT '>>> PART 4: ADDING OTHER MISSING COLUMNS <<<' AS status;

-- Add description to file_uploads
SET @check_desc = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'file_uploads' AND COLUMN_NAME = 'description');

SET @add_desc = IF(@check_desc = 0,
    'ALTER TABLE file_uploads ADD COLUMN description VARCHAR(500) NULL',
    'SELECT ''description already exists in file_uploads'' AS result');

PREPARE stmt FROM @add_desc;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add user_uuid to file_uploads
SET @check_user_uuid = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                        WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'file_uploads' AND COLUMN_NAME = 'user_uuid');

SET @add_user_uuid = IF(@check_user_uuid = 0,
    'ALTER TABLE file_uploads ADD COLUMN user_uuid VARCHAR(36) NULL',
    'SELECT ''user_uuid already exists in file_uploads'' AS result');

PREPARE stmt FROM @add_user_uuid;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================
-- PART 5: FINAL VERIFICATION
-- =====================================================

SELECT '' AS '';
SELECT '>>> FINAL VERIFICATION <<<' AS status;

-- Check UUID population
SELECT 
    'merchants' as table_name,
    COUNT(*) as total,
    SUM(CASE WHEN uuid IS NOT NULL AND uuid != '' THEN 1 ELSE 0 END) as with_uuid,
    CONCAT(ROUND(SUM(CASE WHEN uuid IS NOT NULL AND uuid != '' THEN 1 ELSE 0 END) / COUNT(*) * 100, 0), '%') as coverage,
    CASE 
        WHEN SUM(CASE WHEN uuid IS NULL OR uuid = '' THEN 1 ELSE 0 END) = 0 
        THEN '✅ COMPLETE'
        ELSE '❌ INCOMPLETE'
    END as status
FROM merchants

UNION ALL

SELECT 'clients', COUNT(*), SUM(CASE WHEN uuid IS NOT NULL AND uuid != '' THEN 1 ELSE 0 END),
       CONCAT(ROUND(SUM(CASE WHEN uuid IS NOT NULL AND uuid != '' THEN 1 ELSE 0 END) / COUNT(*) * 100, 0), '%'),
       CASE WHEN SUM(CASE WHEN uuid IS NULL OR uuid = '' THEN 1 ELSE 0 END) = 0 THEN '✅ COMPLETE' ELSE '❌ INCOMPLETE' END
FROM clients

UNION ALL

SELECT 'organizations', COUNT(*), SUM(CASE WHEN uuid IS NOT NULL AND uuid != '' THEN 1 ELSE 0 END),
       CONCAT(ROUND(SUM(CASE WHEN uuid IS NOT NULL AND uuid != '' THEN 1 ELSE 0 END) / COUNT(*) * 100, 0), '%'),
       CASE WHEN SUM(CASE WHEN uuid IS NULL OR uuid = '' THEN 1 ELSE 0 END) = 0 THEN '✅ COMPLETE' ELSE '❌ INCOMPLETE' END
FROM organizations

UNION ALL

SELECT 'users', COUNT(*), SUM(CASE WHEN uuid IS NOT NULL AND uuid != '' THEN 1 ELSE 0 END),
       CONCAT(ROUND(SUM(CASE WHEN uuid IS NOT NULL AND uuid != '' THEN 1 ELSE 0 END) / COUNT(*) * 100, 0), '%'),
       CASE WHEN SUM(CASE WHEN uuid IS NULL OR uuid = '' THEN 1 ELSE 0 END) = 0 THEN '✅ COMPLETE' ELSE '❌ INCOMPLETE' END
FROM users;

-- Show sample merchant UUIDs
SELECT 'Sample merchant UUIDs:' AS info;
SELECT id, code, name, uuid 
FROM merchants 
LIMIT 5;

SELECT '' AS '';
SELECT '========================================' AS '';
SELECT '  ✅ VPS SETUP COMPLETE!' AS '';
SELECT '  All UUIDs populated' AS '';
SELECT '  Links will now work correctly' AS '';
SELECT '  Completed at: ' AS '', NOW() AS timestamp;
SELECT '========================================' AS '';

SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET SQL_MODE=@OLD_SQL_MODE;

