import { test, expect, Page } from '@playwright/test';
import { backOffice, signIn, watchServerErrors, expectNoErrorPage } from './helpers';

// PAYKESHO REGISTRATION: an employee can only register against an employer on
// the platform, with details that match that employer's staff list. The
// registration (name, address, NRC, payslip) then waits for back-office KYC
// review; the client can sign in only once it is approved.
// Staff-list entry seeded in fixtures.sql: Mary Banda / 222222/22/2 / 2000000002 / man number 1002.
test.describe.configure({ mode: 'serial' });

const PNG = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAAABJRU5ErkJggg==', 'base64');
const PDF = Buffer.from('%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF\n');
const MARY = { email: 'mary@ci.paykesho.test', password: 'Mary-Test-2026' };

async function fillRegistration(page: Page, manNumber: string, email: string, opts: { middleName?: string } = {}) {
  await page.goto('/clients/register');
  await page.locator('#salutation').selectOption({ index: 1 });
  await page.locator('#firstName').fill('Mary');
  if (opts.middleName) await page.locator('#middleName').fill(opts.middleName);
  await page.locator('#lastName').fill('Banda');
  await page.locator('#nrc').fill('222222/22/2');
  await page.locator('#phoneNumber').fill('0970000004');
  await page.locator('#tpin').fill('2000000002');
  await page.locator('#manNumber').fill(manNumber);
  await page.locator('#clientIncome').fill('8500');
  await page.locator('#genderType').selectOption({ index: 1 });
  await page.locator('#houseNumber').fill('Plot 12');
  await page.locator('#road').fill('Kabulonga Road');
  await page.locator('#area').fill('Kabulonga, Lusaka');
  const employer = await page.locator('#organization option', { hasText: 'XYZ Limited (CI)' }).getAttribute('value');
  await page.locator('#organization').selectOption(employer!);
  await page.locator('#email').fill(email);
  await page.locator('#password').fill(MARY.password);
  await page.locator('#confirmPassword').fill(MARY.password);
  await page.locator('#nrcFront').setInputFiles({ name: 'nrc-front.png', mimeType: 'image/png', buffer: PNG });
  await page.locator('#nrcBack').setInputFiles({ name: 'nrc-back.png', mimeType: 'image/png', buffer: PNG });
  await page.locator('#payslip').setInputFiles({ name: 'payslip.pdf', mimeType: 'application/pdf', buffer: PDF });
  await page.locator('button[type="submit"]').filter({ hasText: 'Submit' }).click();
  await page.locator('.swal2-confirm').click(); // "Yes, submit"
}

async function trySignIn(page: Page, email: string, password: string) {
  await page.goto('/login');
  await page.locator('input[name="email"]').fill(email);
  await page.locator('input[name="password"]').fill(password);
  const posted = page.waitForResponse((r) => r.request().method() === 'POST' && new URL(r.url()).pathname === '/login');
  await page.locator('form:has(input[name="password"]) [type="submit"]').first().click();
  await posted;
  // The POST redirects to /login?error (refused) or the dashboard; wait for that
  // page rather than reading the sign-in page that is still showing.
  await page.waitForURL((u) => u.pathname !== '/login' || u.search !== '');
  await page.waitForLoadState('load');
}

test('an employee whose details do not match the staff list cannot register', async ({ page }) => {
  await fillRegistration(page, '9999', 'mary.wrong@ci.paykesho.test');
  await expect(page.locator('body')).toContainText(/does not match|do not match/i);
  await trySignIn(page, 'mary.wrong@ci.paykesho.test', MARY.password);
  await expect(page).toHaveURL((u) => u.pathname === '/login');
  await expect(page.locator('input[name="password"]').first()).toBeVisible();
});

test('registration without the NRC and payslip is refused by the server', async ({ page }) => {
  // A direct POST that skips the page's required file inputs.
  await page.goto('/clients/register');
  const csrf = await page.locator('input[name="_csrf"]').first().inputValue();
  const employer = await page.locator('#organization option', { hasText: 'XYZ Limited (CI)' }).getAttribute('value');
  const r = await page.request.post('/clients/save-client', {
    multipart: {
      _csrf: csrf, salutation: 'MS', firstName: 'Mary', lastName: 'Banda', nrc: '222222/22/2',
      phoneNumber: '0970000004', tpin: '2000000002', manNumber: '1002', clientIncome: '8500',
      genderType: 'FEMALE', houseNumber: 'Plot 12', road: 'Kabulonga Road', area: 'Kabulonga',
      organization: employer!, email: 'mary.nodocs@ci.paykesho.test', password: MARY.password,
    },
  });
  expect(r.status()).toBe(200);
  expect(await r.text()).toContain('Please attach your NRC (front)');
  await trySignIn(page, 'mary.nodocs@ci.paykesho.test', MARY.password);
  await expect(page).toHaveURL((u) => u.pathname === '/login');
});

test('an employee on the staff list registers; sign-in waits for KYC review', async ({ page }) => {
  const errors = watchServerErrors(page);
  // Middle name not on the staff list ("Mary Banda") still matches.
  await fillRegistration(page, '1002', MARY.email, { middleName: 'Chanda' });
  await expect(page.locator('body')).toContainText(/Registration received/i, { timeout: 30_000 });

  await trySignIn(page, MARY.email, MARY.password);
  await expect(page).toHaveURL((u) => u.pathname === '/login');
  const shown = (await page.locator('body').innerText()).replace(/\s+/g, ' ');
  expect(shown, `sign-in page shows: ${page.url()} ${shown.slice(0, 400)}`).toMatch(/registration is being reviewed/i);

  // Wrong password: no hint that the account exists.
  await trySignIn(page, MARY.email, 'Wrong-Password-1');
  await expect(page.locator('body')).not.toContainText(/being reviewed/i);
  expect(errors).toEqual([]);
});

test('the back office reviews the documents and approves; the client can then sign in', async ({ page }) => {
  await page.setViewportSize({ width: 1600, height: 1000 });
  const errors = watchServerErrors(page);
  page.on('dialog', (d) => d.accept()); // "Approve this registration and activate the account?"
  await signIn(page, backOffice);

  await page.goto('/admin/kyc');
  await expectNoErrorPage(page);
  const row = page.locator('#kyc-open tr', { hasText: 'Mary Chanda Banda' });
  await expect(row).toHaveCount(1);
  await row.locator('a', { hasText: 'Review' }).click();
  await expectNoErrorPage(page);

  // Staff list comparison and the uploaded documents.
  await expect(page.locator('#kyc-staff-record')).toContainText('Mary Banda');
  const docs = page.locator('#kyc-documents a');
  await expect(docs).toHaveCount(3);
  for (const href of await docs.evaluateAll((as) => as.map((a) => (a as HTMLAnchorElement).getAttribute('href')))) {
    const file = await page.request.get(href!);
    expect(file.status(), `document ${href}`).toBe(200);
    expect(file.headers()['content-type']).toMatch(/image\/png|application\/pdf/);
  }

  await page.locator('#kyc-start').click();
  await page.waitForLoadState('load');
  await expect(page.locator('#kyc-status')).toHaveText('UNDER_REVIEW');

  await page.locator('#kyc-approve').click();
  await page.waitForURL(/\/admin\/kyc$/);
  await expectNoErrorPage(page);
  await expect(page.locator('#kyc-decided tr', { hasText: 'Mary Chanda Banda' })).toContainText('APPROVED');
  expect(errors).toEqual([]);

  // A signed-out visitor cannot open the queue or the documents.
  const anon = await page.context().browser()!.newContext();
  const r = await anon.request.get(new URL('/admin/kyc', page.url()).toString(), { maxRedirects: 0 });
  expect([302, 401, 403]).toContain(r.status());
  await anon.close();
});

test('the approved client signs in', async ({ page }) => {
  await trySignIn(page, MARY.email, MARY.password);
  await expect(page).toHaveURL(/\/clients\/dashboard$/);
});
