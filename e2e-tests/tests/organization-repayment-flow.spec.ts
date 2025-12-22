import { test, expect } from '@playwright/test';

test.describe('Organization Repayment Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Login as organization admin
    await page.goto('/login');
    await page.fill('input[name="email"]', 'orgadmin@example.com');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');
  });

  test('should create repayment schedule successfully', async ({ page }) => {
    // Navigate to repayment schedules
    await page.goto('/repayments/schedules');

    // Fill repayment schedule form
    await page.selectOption('select[name="client"]', '1'); // Select client
    await page.fill('input[name="dueDate"]', '2024-12-31');
    await page.fill('input[name="amountDue"]', '1000.00');

    // Submit form
    await page.click('button[type="submit"][name="save-schedule"]');

    // Verify success
    await expect(page.locator('.alert-success')).toContainText('Repayment schedule created');
  });

  test('should process payroll deductions successfully', async ({ page }) => {
    // Navigate to payroll deductions
    await page.goto('/repayments/payroll-deductions');

    // Verify due schedules are displayed
    await expect(page.locator('table tbody tr')).toHaveCount(1, { timeout: 5000 });

    // Select payment method
    await page.selectOption('select[name="paymentMethod"]', 'Mobile Money');

    // Submit payroll deductions
    await page.click('button[type="submit"][name="save-payroll-deductions"]');

    // Verify success message
    await expect(page.locator('.alert-success')).toContainText('Payroll deductions processed');
  });

  test('should process card payment for repayments', async ({ page }) => {
    await page.goto('/repayments/payroll-deductions');

    // Select card payment method
    await page.selectOption('select[name="paymentMethod"]', 'Card');
    await page.click('button[type="submit"]');

    // Verify redirect to payment gateway
    await expect(page).toHaveURL(/paymentgateway\.com|zynlepay\.com/, { timeout: 10000 });
  });

  test('should view repayment schedules', async ({ page }) => {
    await page.goto('/repayments/schedules');

    // Verify schedules table is displayed
    await expect(page.locator('table')).toBeVisible();
    await expect(page.locator('h1, h2, .page-title')).toContainText('Repayment Schedules');
  });

  test('should view payroll deduction history', async ({ page }) => {
    await page.goto('/repayments/payroll-deductions');

    // Verify payroll deductions table
    await expect(page.locator('table')).toBeVisible();
    await expect(page.locator('.payroll-deduction-list')).toBeVisible();
  });
});
