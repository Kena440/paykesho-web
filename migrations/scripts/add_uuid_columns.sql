-- Migration script to add UUID columns to all entity tables
-- Run this script after deploying the updated entities

-- Add UUID column to users table
ALTER TABLE users ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to merchants table
ALTER TABLE merchants ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to organizations table
ALTER TABLE organizations ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to clients table
ALTER TABLE clients ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to vouchers table
ALTER TABLE vouchers ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to transactions table
ALTER TABLE transactions ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to settlements table
ALTER TABLE settlements ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to payments table
ALTER TABLE payments ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to notifications table
ALTER TABLE notifications ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to notification_queue table
ALTER TABLE notification_queue ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to announcements table
ALTER TABLE announcements ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to employee_records table
ALTER TABLE employee_records ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to merchant_admins table
ALTER TABLE merchant_admins ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to organization_admins table
ALTER TABLE organization_admins ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to repayment_schedules table
ALTER TABLE repayment_schedules ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to merchant_money_account_details table
ALTER TABLE merchant_money_account_details ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to payroll_deductions table
ALTER TABLE payroll_deductions ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to api_keys table
ALTER TABLE api_keys ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to roles table
ALTER TABLE role ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to states table
ALTER TABLE states ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to countries table
ALTER TABLE countries ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to currencies table
ALTER TABLE currencies ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to client_balance table
ALTER TABLE client_balance ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to client_credit_score table
ALTER TABLE client_credit_score ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to employee_payslip_information table
ALTER TABLE employee_payslip_information ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to notification_requests table
ALTER TABLE notification_requests ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to otps table
ALTER TABLE otps ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to device_meta_data table
ALTER TABLE device_meta_data ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Add UUID column to idempotency_records table
ALTER TABLE idempotency_records ADD COLUMN uuid VARCHAR(36) UNIQUE;

-- Generate UUIDs for existing records
-- Users
UPDATE users SET uuid = UUID() WHERE uuid IS NULL;

-- Merchants
UPDATE merchants SET uuid = UUID() WHERE uuid IS NULL;

-- Organizations
UPDATE organizations SET uuid = UUID() WHERE uuid IS NULL;

-- Clients
UPDATE clients SET uuid = UUID() WHERE uuid IS NULL;

-- Vouchers
UPDATE vouchers SET uuid = UUID() WHERE uuid IS NULL;

-- Transactions
UPDATE transactions SET uuid = UUID() WHERE uuid IS NULL;

-- Settlements
UPDATE settlements SET uuid = UUID() WHERE uuid IS NULL;

-- Payments
UPDATE payments SET uuid = UUID() WHERE uuid IS NULL;

-- Notifications
UPDATE notifications SET uuid = UUID() WHERE uuid IS NULL;

-- Notification Queue
UPDATE notification_queue SET uuid = UUID() WHERE uuid IS NULL;

-- Announcements
UPDATE announcements SET uuid = UUID() WHERE uuid IS NULL;

-- Employee Records
UPDATE employee_records SET uuid = UUID() WHERE uuid IS NULL;

-- Merchant Admins
UPDATE merchant_admins SET uuid = UUID() WHERE uuid IS NULL;

-- Organization Admins
UPDATE organization_admins SET uuid = UUID() WHERE uuid IS NULL;

-- Repayment Schedules
UPDATE repayment_schedules SET uuid = UUID() WHERE uuid IS NULL;

-- Merchant Money Account Details
UPDATE merchant_money_account_details SET uuid = UUID() WHERE uuid IS NULL;

-- Payroll Deductions
UPDATE payroll_deductions SET uuid = UUID() WHERE uuid IS NULL;

-- API Keys
UPDATE api_keys SET uuid = UUID() WHERE uuid IS NULL;

-- Roles
UPDATE role SET uuid = UUID() WHERE uuid IS NULL;

-- States
UPDATE states SET uuid = UUID() WHERE uuid IS NULL;

-- Countries
UPDATE countries SET uuid = UUID() WHERE uuid IS NULL;

-- Currencies
UPDATE currencies SET uuid = UUID() WHERE uuid IS NULL;

-- Client Balance
UPDATE client_balance SET uuid = UUID() WHERE uuid IS NULL;

-- Credit Score
UPDATE client_credit_score SET uuid = UUID() WHERE uuid IS NULL;

-- Employee Payslip Information
UPDATE employee_payslip_information SET uuid = UUID() WHERE uuid IS NULL;

-- Notification Requests
UPDATE notification_requests SET uuid = UUID() WHERE uuid IS NULL;

-- OTPs
UPDATE otps SET uuid = UUID() WHERE uuid IS NULL;

-- Device Metadata
UPDATE device_meta_data SET uuid = UUID() WHERE uuid IS NULL;

-- Idempotency Records
UPDATE idempotency_records SET uuid = UUID() WHERE uuid IS NULL;

-- Make UUID columns NOT NULL after populating
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

-- Add indexes for better query performance
CREATE INDEX idx_users_uuid ON users(uuid);
CREATE INDEX idx_merchants_uuid ON merchants(uuid);
CREATE INDEX idx_organizations_uuid ON organizations(uuid);
CREATE INDEX idx_clients_uuid ON clients(uuid);
CREATE INDEX idx_vouchers_uuid ON vouchers(uuid);
CREATE INDEX idx_transactions_uuid ON transactions(uuid);
CREATE INDEX idx_settlements_uuid ON settlements(uuid);
CREATE INDEX idx_payments_uuid ON payments(uuid);
CREATE INDEX idx_notifications_uuid ON notifications(uuid);
CREATE INDEX idx_notification_queue_uuid ON notification_queue(uuid);
CREATE INDEX idx_announcements_uuid ON announcements(uuid);
CREATE INDEX idx_employee_records_uuid ON employee_records(uuid);
CREATE INDEX idx_merchant_admins_uuid ON merchant_admins(uuid);
CREATE INDEX idx_organization_admins_uuid ON organization_admins(uuid);
CREATE INDEX idx_repayment_schedules_uuid ON repayment_schedules(uuid);
CREATE INDEX idx_merchant_money_account_details_uuid ON merchant_money_account_details(uuid);
CREATE INDEX idx_payroll_deductions_uuid ON payroll_deductions(uuid);
CREATE INDEX idx_api_keys_uuid ON api_keys(uuid);
CREATE INDEX idx_role_uuid ON role(uuid);
CREATE INDEX idx_states_uuid ON states(uuid);
CREATE INDEX idx_countries_uuid ON countries(uuid);
CREATE INDEX idx_currencies_uuid ON currencies(uuid);
CREATE INDEX idx_client_balance_uuid ON client_balance(uuid);
CREATE INDEX idx_client_credit_score_uuid ON client_credit_score(uuid);
CREATE INDEX idx_employee_payslip_information_uuid ON employee_payslip_information(uuid);
CREATE INDEX idx_notification_requests_uuid ON notification_requests(uuid);
CREATE INDEX idx_otps_uuid ON otps(uuid);
CREATE INDEX idx_device_meta_data_uuid ON device_meta_data(uuid);
CREATE INDEX idx_idempotency_records_uuid ON idempotency_records(uuid);
