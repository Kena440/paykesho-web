import { test, expect } from '@playwright/test';

test.describe('Organization Client Management Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Login as organization admin
    await page.goto('/login');
    await page.fill('input[name="email"]', 'orgadmin@example.com');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');
  });

  test('should view client list', async ({ page }) => {
    await page.goto('/clients');

    // Verify clients table
    await expect(page.locator('table')).toBeVisible();
    await expect(page.locator('h1, h2, .page-title')).toContainText('Clients');
  });

  test('should view client details', async ({ page }) => {
    await page.goto('/clients');

    // Click on first client
    await page.click('table tbody tr:first-child a[href*="/clients/details/"]');

    // Verify client details page
    await expect(page.locator('.client-details')).toBeVisible();
    await expect(page.locator('.client-balance')).toBeVisible();
  });

  test('should edit client information', async ({ page }) => {
    await page.goto('/clients');

    // Click edit on first client
    await page.click('table tbody tr:first-child a[href*="/clients/edit/"]');

    // Update client information
    await page.fill('input[name="phoneNumber"]', '+260971111111');
    await page.click('button[type="submit"]');

    // Verify success
    await expect(page.locator('.alert-success')).toContainText('Client updated');
  });

  test('should enable/disable client', async ({ page }) => {
    await page.goto('/clients');

    // Get client UUID
    const clientLink = await page.locator('table tbody tr:first-child a[href*="/clients/"]').first().getAttribute('href');
    const clientUuid = clientLink?.split('/').pop();

    // Toggle client status
    await page.goto(`/clients/${clientUuid}/enabled/false`);

    // Verify status change
    await expect(page.locator('.alert-success')).toContainText('Client disabled');
  });
});
