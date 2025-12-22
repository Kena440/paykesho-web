import { test, expect } from '@playwright/test';

test.describe('Transaction Payment Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Login as client
    await page.goto('/login');
    await page.fill('input[name="email"]', 'client@example.com');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');
  });

  test('should initiate and complete transaction successfully', async ({ page }) => {
    // Navigate to initiate transaction page
    await page.goto('/transactions/initiate');

    // Verify vouchers are displayed
    await expect(page.locator('table tbody tr')).toHaveCount(1, { timeout: 5000 });

    // Select merchant and amount (assuming form exists)
    await page.selectOption('select[name="merchantCode"]', 'MERCHANT001');
    await page.fill('input[name="amount"]', '1000.00');

    // Submit transaction
    await page.click('button[type="submit"]');

    // Verify transaction confirmation
    await expect(page.locator('.transaction-confirmation')).toBeVisible();
  });

  test('should prevent duplicate transaction with same idempotency key', async ({ page, request }) => {
    const idempotencyKey = 'test-idempotency-key-123';

    // First transaction
    const response1 = await request.post('/transactions/confirm', {
      headers: {
        'Idempotency-Key': idempotencyKey,
        'Content-Type': 'application/json'
      },
      data: {
        merchantCode: 'MERCHANT001',
        amount: '1000.00'
      }
    });

    expect(response1.ok()).toBeTruthy();

    // Duplicate transaction with same key
    const response2 = await request.post('/transactions/confirm', {
      headers: {
        'Idempotency-Key': idempotencyKey,
        'Content-Type': 'application/json'
      },
      data: {
        merchantCode: 'MERCHANT001',
        amount: '1000.00'
      }
    });

    expect(response2.status()).toBe(409); // Conflict
    const body = await response2.json();
    expect(body.message).toContain('already processed');
  });

  test('should show error for insufficient balance', async ({ page, request }) => {
    const response = await request.post('/transactions/confirm', {
      headers: {
        'Idempotency-Key': 'test-key-insufficient',
        'Content-Type': 'application/json'
      },
      data: {
        merchantCode: 'MERCHANT001',
        amount: '1000000.00' // Very large amount
      }
    });

    expect(response.status()).toBe(400);
    const body = await response.json();
    expect(body.message).toContain('Insufficient balance');
  });

  test('should view transaction history', async ({ page }) => {
    await page.goto('/transactions');

    // Verify transactions table is displayed
    await expect(page.locator('table')).toBeVisible();
    await expect(page.locator('h1, h2, .page-title')).toContainText('Transactions');
  });
});
