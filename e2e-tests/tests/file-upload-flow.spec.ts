import { test, expect } from '@playwright/test';

test.describe('File Upload Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Login as client
    await page.goto('/login');
    await page.fill('input[name="email"]', 'client@example.com');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');
  });

  test('should upload payslip file successfully', async ({ page }) => {
    await page.goto('/vouchers/request');

    // Create a test PDF file
    const fileInput = page.locator('input[type="file"][name="payslip"]');
    await fileInput.setInputFiles({
      name: 'payslip.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from('%PDF-1.4 fake pdf content')
    });

    await page.fill('input[name="amount"]', '5000.00');
    await page.fill('input[name="repaymentCycle"]', '6');
    await page.click('button[type="submit"]');

    // Verify file was uploaded
    await expect(page.locator('.alert-success')).toContainText('Voucher Operation Successful');
  });

  test('should reject invalid file type', async ({ page }) => {
    await page.goto('/vouchers/request');

    // Try to upload executable file
    const fileInput = page.locator('input[type="file"][name="payslip"]');
    await fileInput.setInputFiles({
      name: 'malicious.exe',
      mimeType: 'application/x-executable',
      buffer: Buffer.from('executable content')
    });

    await page.fill('input[name="amount"]', '5000.00');
    await page.click('button[type="submit"]');

    // Verify error
    await expect(page.locator('.alert-danger')).toContainText('Invalid file type');
  });

  test('should reject file that is too large', async ({ page }) => {
    await page.goto('/vouchers/request');

    // Create large file (simulate)
    const largeFileContent = 'x'.repeat(10 * 1024 * 1024); // 10MB
    const fileInput = page.locator('input[type="file"][name="payslip"]');
    
    // Note: Browser will typically prevent this before submission
    await fileInput.setInputFiles({
      name: 'large.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from(largeFileContent)
    });

    await page.fill('input[name="amount"]', '5000.00');
    await page.click('button[type="submit"]');

    // Verify error for file size
    await expect(page.locator('.alert-danger, .error-message')).toContainText(/size|too large/i);
  });

  test('should upload merchant logo in admin portal', async ({ page }) => {
    // Login as admin
    await page.goto('/login');
    await page.fill('input[name="email"]', 'admin@paykesho.com');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');

    await page.goto('/merchants/new');

    // Upload logo
    const fileInput = page.locator('input[type="file"][name="image"]');
    await fileInput.setInputFiles({
      name: 'logo.png',
      mimeType: 'image/png',
      buffer: Buffer.from('fake png content')
    });

    await page.fill('input[name="merchantName"]', 'Test Merchant');
    await page.click('button[type="submit"]');

    // Verify upload success
    await expect(page.locator('.alert-success')).toContainText('Merchant saved');
  });
});
