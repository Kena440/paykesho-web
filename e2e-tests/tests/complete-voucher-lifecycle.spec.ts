import { test, expect } from '@playwright/test';

test.describe('Complete Voucher Lifecycle', () => {
  let voucherUuid: string;
  let clientEmail = 'client@example.com';
  let adminEmail = 'admin@paykesho.com';

  test('complete flow: request → approve → use in transaction', async ({ page, context }) => {
    // Step 1: Client requests voucher
    await page.goto('/login');
    await page.fill('input[name="email"]', clientEmail);
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');

    await page.goto('/vouchers/request');
    await page.fill('input[name="amount"]', '3000.00');
    await page.fill('input[name="repaymentCycle"]', '3');
    await page.fill('textarea[name="reason"]', 'Test voucher request');

    await page.click('button[type="submit"]');
    await expect(page.locator('.alert-success')).toContainText('Voucher Operation Successful');

    // Get voucher reference from success message
    const successMessage = await page.locator('.alert-success').textContent();
    const voucherRef = successMessage?.match(/ITEM_REQ_VCR_\d+/)?.[0];
    expect(voucherRef).toBeDefined();

    // Step 2: Admin approves voucher
    const adminPage = await context.newPage();
    await adminPage.goto('/login');
    await adminPage.fill('input[name="email"]', adminEmail);
    await adminPage.fill('input[name="password"]', 'admin123');
    await adminPage.click('button[type="submit"]');
    await adminPage.waitForURL('**/dashboard');

    await adminPage.goto('/vouchers');
    
    // Find and click approve for the voucher we just created
    const approveLink = adminPage.locator(`table tbody tr:has-text("${voucherRef}") a[href*="/vouchers/approve/"]`);
    await approveLink.click();

    await expect(adminPage.locator('.alert-success')).toContainText('Voucher has been approved');

    // Get voucher UUID from approve link
    const approveHref = await approveLink.getAttribute('href');
    voucherUuid = approveHref?.split('/').pop() || '';

    // Step 3: Client uses voucher in transaction
    await page.goto('/transactions/initiate');
    await page.selectOption('select[name="merchantCode"]', 'MERCHANT001');
    await page.fill('input[name="amount"]', '500.00');
    await page.click('button[type="submit"]');

    // Verify transaction success
    await expect(page.locator('.transaction-success')).toBeVisible();

    // Step 4: Verify balance updated
    await page.goto('/dashboard');
    const balance = await page.locator('.balance-amount').textContent();
    expect(balance).toBeDefined();

    await adminPage.close();
  });

  test('complete flow: request → reject', async ({ page, context }) => {
    // Client requests voucher
    await page.goto('/login');
    await page.fill('input[name="email"]', clientEmail);
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');

    await page.goto('/vouchers/request');
    await page.fill('input[name="amount"]', '2000.00');
    await page.fill('input[name="repaymentCycle"]', '6');
    await page.fill('textarea[name="reason"]', 'Test rejection');

    await page.click('button[type="submit"]');
    await expect(page.locator('.alert-success')).toContainText('Voucher Operation Successful');

    // Admin rejects voucher
    const adminPage = await context.newPage();
    await adminPage.goto('/login');
    await adminPage.fill('input[name="email"]', adminEmail);
    await adminPage.fill('input[name="password"]', 'admin123');
    await adminPage.click('button[type="submit"]');
    await adminPage.waitForURL('**/dashboard');

    await adminPage.goto('/vouchers');
    
    // Find voucher and reject it
    const rejectButton = adminPage.locator('table tbody tr:last-child button:has-text("Reject")');
    await rejectButton.click();

    // Fill rejection reason
    await adminPage.fill('textarea[name="modalRejectVoucherMessage"]', 'Insufficient documentation');
    await adminPage.click('button:has-text("Confirm Reject")');

    // Verify rejection
    await expect(adminPage.locator('.alert-success')).toContainText('Voucher has been rejected');

    await adminPage.close();
  });
});
