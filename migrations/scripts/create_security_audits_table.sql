-- =====================================================
-- Create security_audits table
-- =====================================================
-- This table stores security audit findings from
-- automated scans, allowing admins to review and
-- take action on malicious data
-- =====================================================

USE paykesho_db;

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

-- Add UUID column if not exists (for future compatibility)
SET @dbname = DATABASE();
SET @tablename = 'security_audits';
SET @columnname = 'uuid';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  'SELECT ''UUID column already exists'' AS message;',
  'ALTER TABLE security_audits ADD COLUMN uuid VARCHAR(36) NULL AFTER id;'
));

PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Show table structure
DESCRIBE security_audits;

SELECT 'Security audits table created successfully' AS status;

