import { test, expect } from '@playwright/test';

test.describe('Voucher Request Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Login as client
    await page.goto('/login');
    await page.fill('input[name="email"]', 'client@example.com');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');
  });

  test('should request voucher with payslip upload', async ({ page }) => {
    // Navigate to voucher request page
    await page.goto('/vouchers/request');

    // Fill voucher request form
    await page.fill('input[name="amount"]', '5000.00');
    await page.fill('input[name="repaymentCycle"]', '6');
    await page.fill('textarea[name="reason"]', 'Medical expenses');

    // Upload payslip file
    const fileInput = page.locator('input[type="file"][name="payslip"]');
    await fileInput.setInputFiles({
      name: 'payslip.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from('fake pdf content')
    });

    // Submit form
    await page.click('button[type="submit"]');

    // Verify success message and redirect
    await expect(page).toHaveURL(/\/vouchers$/);
    await expect(page.locator('.alert-success')).toContainText('Voucher Operation Successful');
  });

  test('should prevent voucher request with outstanding debt', async ({ page }) => {
    await page.goto('/vouchers/request');

    // Check if warning message is displayed
    await expect(page.locator('.alert-warning')).toContainText('outstanding debt');
  });

  test('should show error for invalid file type', async ({ page }) => {
    await page.goto('/vouchers/request');

    await page.fill('input[name="amount"]', '5000.00');
    await page.fill('input[name="repaymentCycle"]', '6');

    // Upload invalid file type
    const fileInput = page.locator('input[type="file"][name="payslip"]');
    await fileInput.setInputFiles({
      name: 'document.exe',
      mimeType: 'application/x-executable',
      buffer: Buffer.from('fake exe content')
    });

    await page.click('button[type="submit"]');

    // Verify error message
    await expect(page.locator('.alert-danger')).toContainText('Invalid file type');
  });
});
