import { test, expect } from '@playwright/test';

test.describe('Admin User Management Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Login as admin
    await page.goto('/login');
    await page.fill('input[name="email"]', 'admin@paykesho.com');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');
  });

  test('should create new user successfully', async ({ page }) => {
    await page.goto('/users/new');

    // Fill user form
    await page.fill('input[name="email"]', 'newuser@example.com');
    await page.fill('input[name="firstName"]', 'New');
    await page.fill('input[name="lastName"]', 'User');
    await page.fill('input[name="password"]', 'password123');
    await page.selectOption('select[name="roles"]', '2'); // Select role

    await page.click('button[type="submit"]');

    // Verify success
    await expect(page.locator('.alert-success')).toContainText('User created successfully');
  });

  test('should edit user successfully', async ({ page }) => {
    await page.goto('/users');

    // Click edit on first user
    await page.click('table tbody tr:first-child a[href*="/users/edit/"]');

    // Update user details
    await page.fill('input[name="firstName"]', 'Updated Name');
    await page.click('button[type="submit"]');

    // Verify success
    await expect(page.locator('.alert-success')).toContainText('User updated');
  });

  test('should enable/disable user', async ({ page }) => {
    await page.goto('/users');

    // Get user UUID
    const userLink = await page.locator('table tbody tr:first-child a[href*="/users/"]').first().getAttribute('href');
    const userUuid = userLink?.split('/').pop();

    // Toggle user status
    await page.goto(`/users/${userUuid}/enabled/false`);

    // Verify status change
    await expect(page.locator('.alert-success')).toContainText('User disabled');
  });

  test('should search users', async ({ page }) => {
    await page.goto('/users');

    // Search for user
    await page.fill('input[name="keyword"]', 'test');
    await page.click('button[type="submit"]:has-text("Search")');

    // Verify search results
    await expect(page.locator('table tbody tr')).toHaveCount(1, { timeout: 5000 });
  });
});
