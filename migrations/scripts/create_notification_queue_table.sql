-- =====================================================
-- Create notification_queue table
-- =====================================================
-- This table stores queued notifications (email, SMS, push)
-- Auto-created by Hibernate, but this script can be used
-- if auto-update is disabled or for manual deployment
-- =====================================================

USE paykesho_db;

-- Create notification_queue table
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
    INDEX idx_notification_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Show table structure
DESCRIBE notification_queue;

-- Count existing records
SELECT COUNT(*) as total_notifications FROM notification_queue;

SELECT 'Notification queue table created successfully' AS status;

