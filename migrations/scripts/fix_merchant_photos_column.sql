-- Fix for 'Data too long for column photos' error
-- This script increases the length of photos and photo_path columns across all affected tables
-- Run this script to fix the database schema

-- 1. Merchants table
ALTER TABLE merchants MODIFY COLUMN photos VARCHAR(512);
ALTER TABLE merchants MODIFY COLUMN photo_path VARCHAR(512);

-- 2. Users table
ALTER TABLE users MODIFY COLUMN photos VARCHAR(512);

-- 3. Organizations table
ALTER TABLE organizations MODIFY COLUMN photos VARCHAR(512);
ALTER TABLE organizations MODIFY COLUMN photo_path VARCHAR(512);

-- 4. Clients table
ALTER TABLE clients MODIFY COLUMN photos VARCHAR(512);
ALTER TABLE clients MODIFY COLUMN photo_path VARCHAR(512);

-- 5. Announcements table
ALTER TABLE announcements MODIFY COLUMN photos VARCHAR(512);

-- Verify the changes for all tables
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    COLUMN_TYPE,
    CHARACTER_MAXIMUM_LENGTH
FROM 
    INFORMATION_SCHEMA.COLUMNS
WHERE 
    TABLE_NAME IN ('merchants', 'users', 'organizations', 'clients', 'announcements')
    AND COLUMN_NAME IN ('photos', 'photo_path')
ORDER BY TABLE_NAME, COLUMN_NAME;

