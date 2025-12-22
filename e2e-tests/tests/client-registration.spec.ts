import { test, expect } from '@playwright/test';

test.describe('Client Registration Flow', () => {
  test('should register a new client successfully', async ({ page }) => {
    // Navigate to registration page
    await page.goto('/clients/save-client');

    // Fill registration form
    await page.fill('input[name="email"]', 'test@example.com');
    await page.fill('input[name="firstName"]', 'Test');
    await page.fill('input[name="lastName"]', 'User');
    await page.fill('input[name="password"]', 'password123');
    await page.fill('input[name="phoneNumber"]', '+260971234567');
    await page.fill('input[name="address"]', '123 Test Street');

    // Submit form
    await page.click('button[type="submit"]');

    // Verify success message
    await expect(page.locator('.success-message')).toContainText('Registration successful');
  });

  test('should show error for duplicate email', async ({ page }) => {
    await page.goto('/clients/save-client');

    // Fill form with existing email
    await page.fill('input[name="email"]', 'existing@example.com');
    await page.fill('input[name="firstName"]', 'Test');
    await page.fill('input[name="lastName"]', 'User');
    await page.fill('input[name="password"]', 'password123');

    await page.click('button[type="submit"]');

    // Verify error message
    await expect(page.locator('.error-message')).toContainText('already exists');
  });
});
