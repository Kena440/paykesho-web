import { test, expect } from '@playwright/test';

test.describe('Voucher Approval Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Login as admin
    await page.goto('/login');
    await page.fill('input[name="email"]', 'admin@paykesho.com');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');
  });

  test('should approve voucher successfully', async ({ page }) => {
    // Navigate to vouchers page
    await page.goto('/vouchers');

    // Click on first pending voucher
    await page.click('table tbody tr:first-child a[href*="/vouchers/approve/"]');

    // Verify redirect and success message
    await expect(page).toHaveURL(/\/vouchers$/);
    await expect(page.locator('.alert-success')).toContainText('Voucher has been approved');
  });

  test('should prevent duplicate approval', async ({ page }) => {
    await page.goto('/vouchers');

    // Get voucher UUID
    const voucherLink = await page.locator('table tbody tr:first-child a[href*="/vouchers/approve/"]').getAttribute('href');
    
    // Approve first time
    await page.click('table tbody tr:first-child a[href*="/vouchers/approve/"]');
    await page.waitForTimeout(1000);

    // Try to approve again
    await page.goto(voucherLink!);
    
    // Verify duplicate prevention message
    await expect(page.locator('.alert-warning')).toContainText('already approved');
  });
});
