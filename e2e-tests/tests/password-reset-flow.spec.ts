import { test, expect } from '@playwright/test';

test.describe('Password Reset Flow', () => {
  test('should request password reset successfully', async ({ page }) => {
    // Navigate to forgot password page
    await page.goto('/forgot-password');

    // Enter email
    await page.fill('input[name="email"]', 'client@example.com');
    await page.click('button[type="submit"]');

    // Verify success message
    await expect(page.locator('.alert-success')).toContainText('Password reset link sent');
  });

  test('should reset password with valid token', async ({ page }) => {
    // Navigate to reset password page with token (in real scenario, token comes from email)
    await page.goto('/reset-password?token=valid-reset-token-123');

    // Fill new password
    await page.fill('input[name="password"]', 'newPassword123');
    await page.fill('input[name="confirmPassword"]', 'newPassword123');
    await page.click('button[type="submit"]');

    // Verify success and redirect to login
    await expect(page).toHaveURL(/\/login/);
    await expect(page.locator('.alert-success')).toContainText('Password reset successful');
  });

  test('should show error for invalid reset token', async ({ page }) => {
    await page.goto('/reset-password?token=invalid-token');

    await page.fill('input[name="password"]', 'newPassword123');
    await page.fill('input[name="confirmPassword"]', 'newPassword123');
    await page.click('button[type="submit"]');

    // Verify error message
    await expect(page.locator('.alert-danger')).toContainText('Invalid or expired token');
  });

  test('should show error when passwords do not match', async ({ page }) => {
    await page.goto('/reset-password?token=valid-token');

    await page.fill('input[name="password"]', 'newPassword123');
    await page.fill('input[name="confirmPassword"]', 'differentPassword');
    await page.click('button[type="submit"]');

    // Verify validation error
    await expect(page.locator('.error-message, .alert-danger')).toContainText('do not match');
  });
});
