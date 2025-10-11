-- SAFE MODE COMPATIBLE Migration script to add UUID columns
-- This version works with MySQL safe update mode enabled

-- =============================================================================
-- PHASE 1: Temporarily disable safe mode for this session
-- =============================================================================

SET SQL_SAFE_UPDATES = 0;

-- =============================================================================
-- PHASE 2: Add UUID columns (only if they don't exist)
-- =============================================================================

-- Users
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in users''', 
    'ALTER TABLE users ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Merchants
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchants' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in merchants''', 
    'ALTER TABLE merchants ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Organizations
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'organizations' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in organizations''', 
    'ALTER TABLE organizations ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Clients
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'clients' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in clients''', 
    'ALTER TABLE clients ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Vouchers
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vouchers' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in vouchers''', 
    'ALTER TABLE vouchers ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Transactions
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transactions' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in transactions''', 
    'ALTER TABLE transactions ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Settlements
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'settlements' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in settlements''', 
    'ALTER TABLE settlements ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Payments
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'payments' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in payments''', 
    'ALTER TABLE payments ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Notifications
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in notifications''', 
    'ALTER TABLE notifications ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Notification Queue
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_queue' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in notification_queue''', 
    'ALTER TABLE notification_queue ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Announcements
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'announcements' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in announcements''', 
    'ALTER TABLE announcements ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Employee Records
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'employee_records' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in employee_records''', 
    'ALTER TABLE employee_records ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Merchant Admins
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchant_admins' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in merchant_admins''', 
    'ALTER TABLE merchant_admins ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Organization Admins
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'organization_admins' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in organization_admins''', 
    'ALTER TABLE organization_admins ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Repayment Schedules
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'repayment_schedules' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in repayment_schedules''', 
    'ALTER TABLE repayment_schedules ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Merchant Money Account Details
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchant_money_account_details' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in merchant_money_account_details''', 
    'ALTER TABLE merchant_money_account_details ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Payroll Deductions
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'payroll_deductions' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in payroll_deductions''', 
    'ALTER TABLE payroll_deductions ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- API Keys
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'api_keys' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in api_keys''', 
    'ALTER TABLE api_keys ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Roles
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'role' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in role''', 
    'ALTER TABLE role ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- States
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'states' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in states''', 
    'ALTER TABLE states ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Countries
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'countries' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in countries''', 
    'ALTER TABLE countries ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Currencies
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'currencies' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in currencies''', 
    'ALTER TABLE currencies ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Client Balance
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_balance' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in client_balance''', 
    'ALTER TABLE client_balance ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Client Credit Score
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_credit_score' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in client_credit_score''', 
    'ALTER TABLE client_credit_score ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Employee Payslip Information
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'employee_payslip_information' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in employee_payslip_information''', 
    'ALTER TABLE employee_payslip_information ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Notification Requests
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_requests' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in notification_requests''', 
    'ALTER TABLE notification_requests ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- OTPs
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'otps' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in otps''', 
    'ALTER TABLE otps ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Device Metadata
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'device_meta_data' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in device_meta_data''', 
    'ALTER TABLE device_meta_data ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Idempotency Records
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'idempotency_records' AND COLUMN_NAME = 'uuid');
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column uuid already exists in idempotency_records''', 
    'ALTER TABLE idempotency_records ADD COLUMN uuid VARCHAR(36) UNIQUE');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- =============================================================================
-- PHASE 3: Generate UUIDs for existing records
-- Using the primary key in WHERE clause to satisfy safe mode
-- =============================================================================

-- Users
UPDATE users SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Merchants
UPDATE merchants SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Organizations
UPDATE organizations SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Clients
UPDATE clients SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Vouchers
UPDATE vouchers SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Transactions
UPDATE transactions SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Settlements
UPDATE settlements SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Payments
UPDATE payments SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Notifications
UPDATE notifications SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Notification Queue
UPDATE notification_queue SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Announcements
UPDATE announcements SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Employee Records
UPDATE employee_records SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Merchant Admins
UPDATE merchant_admins SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Organization Admins
UPDATE organization_admins SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Repayment Schedules
UPDATE repayment_schedules SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Merchant Money Account Details
UPDATE merchant_money_account_details SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Payroll Deductions
UPDATE payroll_deductions SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- API Keys
UPDATE api_keys SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Roles
UPDATE role SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- States
UPDATE states SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Countries
UPDATE countries SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Currencies
UPDATE currencies SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Client Balance
UPDATE client_balance SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Credit Score
UPDATE client_credit_score SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Employee Payslip Information
UPDATE employee_payslip_information SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Notification Requests
UPDATE notification_requests SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- OTPs
UPDATE otps SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Device Metadata
UPDATE device_meta_data SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- Idempotency Records
UPDATE idempotency_records SET uuid = UUID() WHERE uuid IS NULL AND id > 0;

-- =============================================================================
-- PHASE 4: Make UUID columns NOT NULL (only if not already)
-- =============================================================================

ALTER TABLE users MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE merchants MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE organizations MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE clients MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE vouchers MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE transactions MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE settlements MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE payments MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE notifications MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE notification_queue MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE announcements MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE employee_records MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE merchant_admins MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE organization_admins MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE repayment_schedules MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE merchant_money_account_details MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE payroll_deductions MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE api_keys MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE role MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE states MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE countries MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE currencies MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE client_balance MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE client_credit_score MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE employee_payslip_information MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE notification_requests MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE otps MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE device_meta_data MODIFY COLUMN uuid VARCHAR(36) NOT NULL;
ALTER TABLE idempotency_records MODIFY COLUMN uuid VARCHAR(36) NOT NULL;

-- =============================================================================
-- PHASE 5: Add indexes for better query performance
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_users_uuid ON users(uuid);
CREATE INDEX IF NOT EXISTS idx_merchants_uuid ON merchants(uuid);
CREATE INDEX IF NOT EXISTS idx_organizations_uuid ON organizations(uuid);
CREATE INDEX IF NOT EXISTS idx_clients_uuid ON clients(uuid);
CREATE INDEX IF NOT EXISTS idx_vouchers_uuid ON vouchers(uuid);
CREATE INDEX IF NOT EXISTS idx_transactions_uuid ON transactions(uuid);
CREATE INDEX IF NOT EXISTS idx_settlements_uuid ON settlements(uuid);
CREATE INDEX IF NOT EXISTS idx_payments_uuid ON payments(uuid);
CREATE INDEX IF NOT EXISTS idx_notifications_uuid ON notifications(uuid);
CREATE INDEX IF NOT EXISTS idx_notification_queue_uuid ON notification_queue(uuid);
CREATE INDEX IF NOT EXISTS idx_announcements_uuid ON announcements(uuid);
CREATE INDEX IF NOT EXISTS idx_employee_records_uuid ON employee_records(uuid);
CREATE INDEX IF NOT EXISTS idx_merchant_admins_uuid ON merchant_admins(uuid);
CREATE INDEX IF NOT EXISTS idx_organization_admins_uuid ON organization_admins(uuid);
CREATE INDEX IF NOT EXISTS idx_repayment_schedules_uuid ON repayment_schedules(uuid);
CREATE INDEX IF NOT EXISTS idx_merchant_money_account_details_uuid ON merchant_money_account_details(uuid);
CREATE INDEX IF NOT EXISTS idx_payroll_deductions_uuid ON payroll_deductions(uuid);
CREATE INDEX IF NOT EXISTS idx_api_keys_uuid ON api_keys(uuid);
CREATE INDEX IF NOT EXISTS idx_role_uuid ON role(uuid);
CREATE INDEX IF NOT EXISTS idx_states_uuid ON states(uuid);
CREATE INDEX IF NOT EXISTS idx_countries_uuid ON countries(uuid);
CREATE INDEX IF NOT EXISTS idx_currencies_uuid ON currencies(uuid);
CREATE INDEX IF NOT EXISTS idx_client_balance_uuid ON client_balance(uuid);
CREATE INDEX IF NOT EXISTS idx_client_credit_score_uuid ON client_credit_score(uuid);
CREATE INDEX IF NOT EXISTS idx_employee_payslip_information_uuid ON employee_payslip_information(uuid);
CREATE INDEX IF NOT EXISTS idx_notification_requests_uuid ON notification_requests(uuid);
CREATE INDEX IF NOT EXISTS idx_otps_uuid ON otps(uuid);
CREATE INDEX IF NOT EXISTS idx_device_meta_data_uuid ON device_meta_data(uuid);
CREATE INDEX IF NOT EXISTS idx_idempotency_records_uuid ON idempotency_records(uuid);

-- =============================================================================
-- PHASE 6: Re-enable safe mode
-- =============================================================================

SET SQL_SAFE_UPDATES = 1;

-- Migration complete!
SELECT 'UUID migration completed successfully! Safe mode re-enabled.' AS Status;

