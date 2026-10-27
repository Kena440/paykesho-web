import { test, expect, Page } from '@playwright/test';
import { backOffice, employerPortal, merchantPortal, signIn, watchServerErrors, expectNoErrorPage } from './helpers';

// What testers do first on an empty system: the back office onboards an
// employer (with its staff list and a portal admin) and a merchant (with payout
// details and a portal admin), and the new admins sign in to their portals with
// the one-time password the back office shows.
test.describe.configure({ mode: 'serial' });

const LOGO = {
  name: 'logo.png',
  mimeType: 'image/png',
  buffer: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==', 'base64'),
};
const stamp = Date.now().toString().slice(-6);
const EMPLOYER = `Onboarding Employer ${stamp}`;
const MERCHANT = `Onboarding Shop ${stamp}`;
const employerAdmin = { email: `hr.${stamp}@onboarding.test`, password: '' };
const merchantAdmin = { email: `owner.${stamp}@onboarding.test`, password: '' };

async function addAdmin(page: Page, name: string, email: string, phone: string): Promise<string> {
  await page.locator('[data-bs-target="#modal-add-admin-record"]').first().click();
  const modal = page.locator('#modal-add-admin-record');
  await expect(modal).toBeVisible();
  await modal.locator('#title').selectOption('Mr.');
  await modal.locator('#name').fill(name);
  await modal.locator('#email').fill(email);
  await modal.locator('#phoneNumber').fill(phone);
  await modal.locator('#saveAdminBtn').click();
  await page.waitForLoadState('load');
  await expectNoErrorPage(page);
  const shown = page.locator('.pk-flash code');
  await expect(shown, 'one-time password shown to the back office').toHaveCount(1);
  return (await shown.innerText()).trim();
}

test('back office onboards an employer with a staff list and an admin', async ({ page }) => {
  await page.setViewportSize({ width: 1600, height: 1000 });
  const errors = watchServerErrors(page);
  await signIn(page, backOffice);

  await page.goto('/admin/organizations/new');
  await page.locator('#name').fill(EMPLOYER);
  await page.locator('#phoneNumber').fill('0970001111');
  await page.locator('#email').fill(`hr-${stamp}@employer.test`);
  await page.locator('#fileImage').setInputFiles(LOGO);
  await page.locator('form button[type="submit"]').filter({ hasText: 'Submit' }).click();
  await page.waitForURL(/\/admin\/organizations$/);
  await expectNoErrorPage(page);

  const row = page.locator('tr', { hasText: EMPLOYER });
  await expect(row).toHaveCount(1);
  await expect(row.locator('img')).toHaveJSProperty('complete', true);
  await row.locator('a[href*="/organizations/view/"]').first().click();
  await expectNoErrorPage(page);

  // Staff list entry the employee will register against.
  await page.locator('#employees-tab').click();
  await page.locator('[data-bs-target="#modal-add-record"]').click();
  const record = page.locator('#modal-add-record');
  await record.locator('input[name="name"]').fill('Peter Phiri');
  await record.locator('input[name="nrc"]').fill('333333/33/3');
  await record.locator('input[name="tpin"]').fill('3000000003');
  await record.locator('input[name="manNumber"]').fill('1003');
  await record.locator('button[type="submit"]').click();
  await page.waitForLoadState('load');
  await expectNoErrorPage(page);

  employerAdmin.password = await addAdmin(page, 'Grace Mwale', employerAdmin.email, '0970001112');
  expect(employerAdmin.password.length).toBeGreaterThanOrEqual(8);
  expect(errors).toEqual([]);
});

test('the new employer admin signs in to the employer portal', async ({ page }) => {
  const errors = watchServerErrors(page);
  await signIn(page, employerPortal, employerAdmin.password, employerAdmin.email);
  await expect(page).toHaveURL(employerPortal.landing);
  await expectNoErrorPage(page);
  expect(errors).toEqual([]);
});

test('back office onboards a merchant with payout details and an admin', async ({ page }) => {
  await page.setViewportSize({ width: 1600, height: 1000 });
  const errors = watchServerErrors(page);
  await signIn(page, backOffice);

  await page.goto('/admin/merchants/new');
  await page.locator('#name').fill(MERCHANT);
  await page.locator('select[name="merchantType"]').selectOption({ index: 1 });
  await page.locator('#phoneNumber').fill('0970002221');
  await page.locator('#email').fill(`shop-${stamp}@merchant.test`);
  await page.locator('#details').fill('Groceries and household goods');
  await page.locator('#servicesOffered').fill('Groceries');
  await page.locator('#fileImage').setInputFiles(LOGO);
  await page.locator('#payoutMethod').selectOption('mobile-money');
  await page.locator('select[name="payoutOperator"]').selectOption({ index: 1 });
  await page.locator('input[name="payoutPhone"]').fill('0970002222');
  await page.locator('#submitBtn').click();
  await page.waitForURL(/\/admin\/merchants$/);
  await expectNoErrorPage(page);

  const row = page.locator('tr', { hasText: MERCHANT });
  await expect(row).toHaveCount(1);
  await row.locator('a[href*="/merchants/view/"]').first().click();
  await expectNoErrorPage(page);

  merchantAdmin.password = await addAdmin(page, 'John Banda', merchantAdmin.email, '0970002223');
  expect(errors).toEqual([]);
});

test('the new merchant admin signs in and sees their shop and QR code', async ({ page }) => {
  const errors = watchServerErrors(page);
  await signIn(page, merchantPortal, merchantAdmin.password, merchantAdmin.email);
  await expect(page).toHaveURL(merchantPortal.landing);
  await expectNoErrorPage(page);
  const info = await (await page.request.get('/merchant/api/merchant/info')).json();
  expect(info.name).toBe(MERCHANT);
  const qr = await page.request.get(`/merchant/generateQRCode?qrContent=${encodeURIComponent(info.code)}`);
  expect(qr.status()).toBe(200);
  expect(qr.headers()['content-type']).toContain('image/png');
  expect(errors).toEqual([]);
});
