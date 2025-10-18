-- =====================================================
-- CREATE ALL MISSING TABLES ON VPS
-- =====================================================
-- This script creates any tables that don't exist yet
-- Safe to run multiple times (uses IF NOT EXISTS)
-- Particularly useful for new security tables
-- =====================================================

USE paykesho_db;

SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO';

-- =====================================================
-- CHECK EXISTING TABLES
-- =====================================================

SELECT '======= CHECKING EXISTING TABLES =======' AS status;

SELECT 
    CASE 
        WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = 'security_audits')
        THEN '✓ EXISTS'
        ELSE '✗ MISSING - Will create'
    END AS security_audits,
    CASE 
        WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = 'notification_queue')
        THEN '✓ EXISTS'
        ELSE '✗ MISSING - Will create'
    END AS notification_queue,
    CASE 
        WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = 'clients')
        THEN '✓ EXISTS'
        ELSE '✗ MISSING'
    END AS clients,
    CASE 
        WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'paykesho_db' AND table_name = 'announcements')
        THEN '✓ EXISTS'
        ELSE '✗ MISSING'
    END AS announcements;

-- =====================================================
-- CREATE SECURITY_AUDITS TABLE (IF NOT EXISTS)
-- =====================================================

SELECT '======= CREATING SECURITY_AUDITS TABLE =======' AS status;

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

-- =====================================================
-- CREATE NOTIFICATION_QUEUE TABLE (IF NOT EXISTS)
-- =====================================================

SELECT '======= CREATING NOTIFICATION_QUEUE TABLE =======' AS status;

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
    
    -- Indexes for performance
    INDEX idx_notification_status (status),
    INDEX idx_notification_type (notification_type),
    INDEX idx_notification_recipient (recipient_email),
    INDEX idx_notification_scheduled (scheduled_for),
    INDEX idx_notification_created (created_at),
    INDEX idx_notification_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- VERIFY TABLE CREATION
-- =====================================================

SELECT '======= VERIFICATION =======' AS status;

SELECT 
    table_name,
    table_rows,
    ROUND((data_length + index_length) / 1024 / 1024, 2) AS size_mb,
    create_time,
    update_time
FROM information_schema.tables
WHERE table_schema = 'paykesho_db'
  AND table_name IN ('security_audits', 'notification_queue')
ORDER BY table_name;

-- Show columns for security_audits
SELECT '--- security_audits columns ---' AS info;
SELECT column_name, column_type, is_nullable, column_key
FROM information_schema.columns
WHERE table_schema = 'paykesho_db' 
  AND table_name = 'security_audits'
ORDER BY ordinal_position;

-- Show columns for notification_queue
SELECT '--- notification_queue columns ---' AS info;
SELECT column_name, column_type, is_nullable, column_key
FROM information_schema.columns
WHERE table_schema = 'paykesho_db' 
  AND table_name = 'notification_queue'
ORDER BY ordinal_position;

-- Count records
SELECT 
    'security_audits' as table_name,
    COUNT(*) as record_count
FROM security_audits
UNION ALL
SELECT 
    'notification_queue' as table_name,
    COUNT(*) as record_count
FROM notification_queue;

SELECT '======= ALL MISSING TABLES CREATED SUCCESSFULLY =======' AS status;

SET SQL_MODE=@OLD_SQL_MODE;

