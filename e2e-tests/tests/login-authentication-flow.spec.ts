import { test, expect } from '@playwright/test';

test.describe('Login Authentication Flow', () => {
  test('should login to client portal successfully', async ({ page }) => {
    await page.goto('/login');

    await page.fill('input[name="email"]', 'client@example.com');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');

    // Verify redirect to dashboard
    await expect(page).toHaveURL(/\/dashboard/);
    await expect(page.locator('.user-info, .dashboard')).toBeVisible();
  });

  test('should login to backoffice successfully', async ({ page }) => {
    await page.goto('/login');

    await page.fill('input[name="email"]', 'admin@paykesho.com');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');

    await expect(page).toHaveURL(/\/dashboard/);
  });

  test('should show error for invalid credentials', async ({ page }) => {
    await page.goto('/login');

    await page.fill('input[name="email"]', 'invalid@example.com');
    await page.fill('input[name="password"]', 'wrongpassword');
    await page.click('button[type="submit"]');

    // Verify error message
    await expect(page.locator('.alert-danger, .error-message')).toContainText('Invalid credentials');
  });

  test('should show error for locked account', async ({ page }) => {
    await page.goto('/login');

    await page.fill('input[name="email"]', 'locked@example.com');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');

    // Verify account locked message
    await expect(page.locator('.alert-warning, .error-message')).toContainText('account.*locked', { timeout: 5000 });
  });

  test('should logout successfully', async ({ page }) => {
    // Login first
    await page.goto('/login');
    await page.fill('input[name="email"]', 'client@example.com');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');

    // Logout
    await page.click('a[href*="/logout"], button[type="button"]:has-text("Logout")');

    // Verify redirect to login
    await expect(page).toHaveURL(/\/login/);
  });
});
