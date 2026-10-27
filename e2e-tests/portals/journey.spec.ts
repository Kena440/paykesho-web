import { test, expect, Page } from '@playwright/test';
import { backOffice, clientPortal, employerPortal, merchantPortal, signIn, watchServerErrors, expectNoErrorPage } from './helpers';

// The PAYKESHO customer journey across all four portals, driven through the
// screens each user sees:
//   client requests an advance with a payslip -> back office reviews the payslip
//   and approves -> client pays the merchant (merchant code, as from the QR) ->
//   balance drops -> merchant, employer and back office all see the result.
// Merchant settlement goes to the Lenco test double (scripts/lenco-stub.mjs).
test.describe.configure({ mode: 'serial' });

const ADVANCE = 500;
const PURCHASE = 120;
const PAYSLIP = {
  name: 'payslip-thomas.pdf',
  mimeType: 'application/pdf',
  buffer: Buffer.from('%PDF-1.4\n1 0 obj<</Type/Catalog>>endobj\ntrailer<</Root 1 0 R>>\n%%EOF\n'),
};

let voucherRef = '';
let paymentRef = '';

async function clientBalance(page: Page): Promise<number> {
  const r = await page.request.get('/api/client/balance');
  expect(r.status()).toBe(200);
  const body = await r.json();
  return Number(body.balance ?? body.availableBalance ?? String(body.formattedBalance).replace(/[^0-9.]/g, ''));
}

test('client requests an advance with a payslip', async ({ page }) => {
  const errors = watchServerErrors(page);
  await signIn(page, clientPortal);
  await page.goto('/vouchers/request');
  await expectNoErrorPage(page);
  await page.locator('#amount').fill(String(ADVANCE));
  await page.locator('#reason').fill('Groceries before payday');
  await page.locator('#repaymentCycle').selectOption('1');
  await page.locator('#payslip').setInputFiles(PAYSLIP);
  await page.locator('form[enctype="multipart/form-data"] button[type="submit"]').click();
  await page.locator('.swal2-confirm').click();
  await page.waitForURL(/\/vouchers$/);
  const body = page.locator('body');
  await expect(body).not.toContainText(/Error saving voucher/i);
  await expect(body).toContainText(/Voucher Operation Successful/i);
  voucherRef = ((await body.innerText()).match(/Voucher Operation Successful:\s*(\S+)/) || [])[1] || '';
  expect(voucherRef, 'voucher reference shown to the client').not.toBe('');
  expect(errors).toEqual([]);
});

test('back office opens the payslip and approves the advance', async ({ page }) => {
  const errors = watchServerErrors(page);
  await signIn(page, backOffice);
  await page.goto('/admin/vouchers');
  await expectNoErrorPage(page);
  const row = page.locator('tr', { hasText: voucherRef });
  await expect(row).toHaveCount(1);

  // The reviewer can open the payslip the client attached.
  const payslipLink = row.locator('[data-voucher-payslip-url]');
  await expect(payslipLink).toHaveCount(1);
  const payslipUrl = await payslipLink.getAttribute('data-voucher-payslip-url');
  const payslip = await page.request.get(new URL(payslipUrl!, page.url()).toString());
  expect(payslip.status(), 'payslip download').toBe(200);
  expect(await payslip.body()).toEqual(PAYSLIP.buffer);

  await row.locator('.approve-button').first().click();
  await page.locator('.swal2-confirm').click();
  await page.waitForURL(/\/admin\/vouchers/);
  await expectNoErrorPage(page);
  await expect(page.locator('tr', { hasText: voucherRef })).toContainText('APPROVED');
  expect(errors).toEqual([]);
});

test('client sees the e-value and pays the merchant', async ({ page }) => {
  const errors = watchServerErrors(page);
  await signIn(page, clientPortal);
  const before = await clientBalance(page);
  expect(before).toBeGreaterThanOrEqual(ADVANCE);

  await page.goto('/transactions/initiate');
  await expectNoErrorPage(page);
  await page.locator('#merchantCode').fill('CIMERCH001');
  await expect(page.locator('#merchantName')).toHaveText('CI Grocers');
  await page.locator('#amount').fill(String(PURCHASE));
  await page.locator('#submitPayment').click();
  const confirmed = page.waitForResponse((r) => new URL(r.url()).pathname === '/transactions/confirm');
  await page.locator('.swal2-confirm').click();
  const res = await confirmed;
  const result = await res.json();
  expect(res.status(), JSON.stringify(result)).toBe(200);
  expect(result.success).toBe(true);
  expect(result.settled).toBe(true);
  paymentRef = result.transactionRef;

  const after = await clientBalance(page);
  expect(after).toBeCloseTo(before - PURCHASE, 2);

  await page.goto('/transactions');
  await expectNoErrorPage(page);
  await expect(page.locator('body')).toContainText(paymentRef);
  expect(errors).toEqual([]);
});

test('a replayed payment request is not charged twice', async ({ page }) => {
  await signIn(page, clientPortal);
  await page.goto('/transactions/initiate');
  const before = await clientBalance(page);
  const token = await page.locator('meta[name="_csrf"]').getAttribute('content');
  const header = await page.locator('meta[name="_csrf_header"]').getAttribute('content');
  const key = `journey-replay-${Date.now()}`;
  const post = () => page.request.post('/transactions/confirm', {
    headers: { 'Idempotency-Key': key, [header!]: token! },
    data: { merchantCode: 'CIMERCH001', amount: '20' },
  });
  const first = await post();
  expect(first.status(), await first.text()).toBe(200);
  const second = await post();
  expect(second.status()).toBe(409);
  expect(await clientBalance(page)).toBeCloseTo(before - 20, 2);
});

test('merchant sees the payment', async ({ page }) => {
  const errors = watchServerErrors(page);
  await signIn(page, merchantPortal);
  await page.goto('/merchant/transactions');
  await expectNoErrorPage(page);
  await expect(page.locator('body')).toContainText(paymentRef);
  const summary = await (await page.request.get('/merchant/api/transactions/summary')).json();
  expect(summary.totalTransactions).toBeGreaterThanOrEqual(1);
  expect(errors).toEqual([]);
});

test('back office sees the payment and its settlement', async ({ page }) => {
  const errors = watchServerErrors(page);
  await signIn(page, backOffice);
  await page.goto('/admin/transactions');
  await expectNoErrorPage(page);
  await expect(page.locator('body')).toContainText(paymentRef);
  expect(errors).toEqual([]);
});

test('employer sees the advance but not what was bought', async ({ page }) => {
  const errors = watchServerErrors(page);
  await signIn(page, employerPortal);
  await page.goto('/employer/vouchers');
  await expectNoErrorPage(page);
  await expect(page.locator('body')).toContainText(voucherRef);
  await expect(page.locator('body')).not.toContainText(paymentRef);
  await expect(page.locator('body')).not.toContainText('CI Grocers');
  expect(errors).toEqual([]);
});
