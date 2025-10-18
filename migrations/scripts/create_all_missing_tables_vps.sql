-- =====================================================
-- CREATE ALL MISSING TABLES FOR VPS DEPLOYMENT
-- =====================================================
-- This script creates all tables that might be missing
-- Safe to run multiple times (uses IF NOT EXISTS)
-- Run this on VPS before deploying new code
-- =====================================================

USE paykesho_db;

SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO';

SELECT '======= CREATING MISSING TABLES =======' AS status;

-- =====================================================
-- 1. SECURITY_AUDITS TABLE (NEW)
-- =====================================================

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
    INDEX idx_severity (severity),
    INDEX idx_reviewed_date (reviewed_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT 'Created: security_audits' AS table_created;

-- =====================================================
-- 2. NOTIFICATION_QUEUE TABLE (NEW)
-- =====================================================

CREATE TABLE IF NOT EXISTS notification_queue (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NULL,
    
    -- Timestamps
    created_at DATETIME NULL,
    scheduled_for DATETIME NULL,
    sent_at DATETIME NULL,
    processing_started_at DATETIME NULL,
    
    -- Notification details
    notification_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    -- Recipient information
    recipient_email VARCHAR(100) NOT NULL,
    recipient_name VARCHAR(128) NULL,
    
    -- Content
    subject VARCHAR(255) NULL,
    content TEXT NULL,
    
    -- Template
    template_name VARCHAR(100) NULL,
    template_variables TEXT NULL,
    
    -- Processing
    priority INT NOT NULL DEFAULT 3,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    error_message TEXT NULL,
    
    -- Metadata
    metadata TEXT NULL,
    entity_reference VARCHAR(100) NULL,
    entity_type VARCHAR(50) NULL,
    
    INDEX idx_notification_status (status),
    INDEX idx_notification_type (notification_type),
    INDEX idx_notification_recipient (recipient_email),
    INDEX idx_notification_scheduled (scheduled_for),
    INDEX idx_notification_created (created_at),
    INDEX idx_notification_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT 'Created: notification_queue' AS table_created;

-- =====================================================
-- 3. ADD MISSING COLUMNS TO EXISTING TABLES
-- =====================================================

SELECT '======= ADDING MISSING COLUMNS =======' AS status;

-- Add UUID columns if missing (for all main tables)
SET @dbname = DATABASE();

-- Clients UUID
SET @tablename = 'clients';
SET @columnname = 'uuid';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT ''uuid already exists in clients'' AS message;',
  'ALTER TABLE clients ADD COLUMN uuid VARCHAR(36) NULL AFTER id, ADD UNIQUE INDEX idx_client_uuid (uuid);'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Users UUID
SET @tablename = 'users';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT ''uuid already exists in users'' AS message;',
  'ALTER TABLE users ADD COLUMN uuid VARCHAR(36) NULL AFTER id, ADD UNIQUE INDEX idx_user_uuid (uuid);'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Organizations UUID
SET @tablename = 'organizations';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT ''uuid already exists in organizations'' AS message;',
  'ALTER TABLE organizations ADD COLUMN uuid VARCHAR(36) NULL AFTER id, ADD UNIQUE INDEX idx_organization_uuid (uuid);'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Merchants UUID
SET @tablename = 'merchants';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT ''uuid already exists in merchants'' AS message;',
  'ALTER TABLE merchants ADD COLUMN uuid VARCHAR(36) NULL AFTER id, ADD UNIQUE INDEX idx_merchant_uuid (uuid);'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add description to file_uploads if missing
SET @tablename = 'file_uploads';
SET @columnname = 'description';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT ''description already exists in file_uploads'' AS message;',
  'ALTER TABLE file_uploads ADD COLUMN description VARCHAR(500) NULL;'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add user_uuid to file_uploads if missing
SET @columnname = 'user_uuid';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT ''user_uuid already exists in file_uploads'' AS message;',
  'ALTER TABLE file_uploads ADD COLUMN user_uuid VARCHAR(36) NULL;'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SELECT 'Missing columns added successfully' AS status;

-- =====================================================
-- 4. FINAL VERIFICATION
-- =====================================================

SELECT '======= FINAL TABLE LIST =======' AS status;

-- Show all tables in database
SELECT 
    table_name,
    table_rows as row_count,
    ROUND((data_length + index_length) / 1024 / 1024, 2) AS size_mb,
    engine,
    table_collation,
    create_time
FROM information_schema.tables
WHERE table_schema = 'paykesho_db'
  AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- Show newly created tables
SELECT '======= NEW SECURITY TABLES =======' AS status;

SELECT 
    table_name,
    'NEW' as type,
    table_rows,
    create_time
FROM information_schema.tables
WHERE table_schema = 'paykesho_db'
  AND table_name IN ('security_audits', 'notification_queue');

SELECT '======= SETUP COMPLETE =======' AS status;
SELECT 'All missing tables have been created!' AS message;
SELECT 'You can now restart your services safely.' AS next_step;

SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET SQL_MODE=@OLD_SQL_MODE;

