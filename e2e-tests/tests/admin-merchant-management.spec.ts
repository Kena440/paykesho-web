import { test, expect } from '@playwright/test';

test.describe('Admin Merchant Management Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Login as admin
    await page.goto('/login');
    await page.fill('input[name="email"]', 'admin@paykesho.com');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');
  });

  test('should create new merchant successfully', async ({ page }) => {
    // Navigate to new merchant page
    await page.goto('/merchants/new');

    // Fill merchant form
    await page.fill('input[name="merchantName"]', 'Test Merchant');
    await page.fill('input[name="merchantCode"]', 'TEST_MERCHANT_001');
    await page.selectOption('select[name="merchantType"]', 'RETAIL');
    await page.fill('input[name="email"]', 'merchant@test.com');
    await page.fill('input[name="phoneNumber"]', '+260971234567');
    await page.fill('textarea[name="address"]', '123 Merchant Street');

    // Upload logo
    const fileInput = page.locator('input[type="file"][name="image"]');
    await fileInput.setInputFiles({
      name: 'logo.png',
      mimeType: 'image/png',
      buffer: Buffer.from('fake png content')
    });

    // Submit form
    await page.click('button[type="submit"]');

    // Verify success and redirect
    await expect(page).toHaveURL(/\/merchants$/);
    await expect(page.locator('.alert-success')).toContainText('Merchant saved successfully');
  });

  test('should edit merchant successfully', async ({ page }) => {
    // Navigate to merchants list
    await page.goto('/merchants');

    // Click edit on first merchant
    await page.click('table tbody tr:first-child a[href*="/merchants/edit/"]');

    // Update merchant name
    await page.fill('input[name="merchantName"]', 'Updated Merchant Name');
    await page.click('button[type="submit"]');

    // Verify success
    await expect(page.locator('.alert-success')).toContainText('Merchant updated');
  });

  test('should enable/disable merchant', async ({ page }) => {
    await page.goto('/merchants');

    // Get merchant UUID from first row
    const merchantLink = await page.locator('table tbody tr:first-child a[href*="/merchants/"]').first().getAttribute('href');
    const merchantUuid = merchantLink?.split('/').pop();

    // Toggle merchant status
    await page.goto(`/merchants/${merchantUuid}/enabled/false`);

    // Verify status change
    await expect(page.locator('.alert-success')).toContainText('Merchant disabled');
  });

  test('should view merchant details', async ({ page }) => {
    await page.goto('/merchants');

    // Click on first merchant to view details
    await page.click('table tbody tr:first-child a[href*="/merchants/view/"]');

    // Verify merchant details page
    await expect(page.locator('h1, h2')).toContainText('Merchant Details');
    await expect(page.locator('.merchant-info')).toBeVisible();
  });
});
