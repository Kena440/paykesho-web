-- Test user creation script for backoffice login testing
-- This creates a test user with email: test@paykesho.com and password: test123

-- Insert test user (password is BCrypt encoded "test123")
INSERT INTO users (id, email, password, first_name, last_name, enabled, created_time, failed_attempt, account_non_locked, lock_time) 
VALUES (999, 'test@paykesho.com', '$2a$10$EblZqNptyYdFMS7DGHKOSu.fGnwME9JXpnLQZjMbKL.7WK0Muf/Pu', 'Test', 'User', 1, NOW(), 0, 1, NULL)
ON DUPLICATE KEY UPDATE 
    password = '$2a$10$EblZqNptyYdFMS7DGHKOSu.fGnwME9JXpnLQZjMbKL.7WK0Muf/Pu',
    enabled = 1,
    failed_attempt = 0,
    account_non_locked = 1,
    lock_time = NULL;

-- Insert admin role if it doesn't exist
INSERT IGNORE INTO roles (id, name, description) VALUES (1, 'Admin', 'Administrator role');

-- Associate user with admin role
INSERT IGNORE INTO users_roles (user_id, role_id) VALUES (999, 1);

-- Verify the user was created
SELECT u.id, u.email, u.first_name, u.last_name, u.enabled, u.account_non_locked, r.name as role
FROM users u 
LEFT JOIN users_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.email = 'test@paykesho.com';
