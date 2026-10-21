import { test, expect, Page } from '@playwright/test';

// PAYKESHO REGISTRATION: an employee can only register against an employer on
// the platform, with details that match that employer's staff list.
// Staff-list entry seeded in fixtures.sql: Mary Banda / 222222/22/2 / 2000000002 / man number 1002.

async function fillRegistration(page: Page, manNumber: string, email: string) {
  await page.goto('/clients/register');
  await page.locator('#salutation').selectOption({ index: 1 });
  await page.locator('#name').fill('Mary Banda');
  await page.locator('#nrc').fill('222222/22/2');
  await page.locator('#phoneNumber').fill('0970000004');
  await page.locator('#tpin').fill('2000000002');
  await page.locator('#manNumber').fill(manNumber);
  await page.locator('#clientIncome').fill('8500');
  await page.locator('#genderType').selectOption({ index: 1 });
  await page.locator('#address').fill('Plot 12, Kabulonga Road, Lusaka');
  const employer = await page.locator('#organization option', { hasText: 'XYZ Limited (CI)' }).getAttribute('value');
  await page.locator('#organization').selectOption(employer!);
  await page.locator('#email').fill(email);
  await page.locator('#password').fill('Mary-Test-2026');
  await page.locator('#confirmPassword').fill('Mary-Test-2026');
  await page.locator('button[type="submit"]').filter({ hasText: 'Submit' }).click();
  await page.locator('.swal2-confirm').click(); // "Yes, submit"
}

test('an employee whose details do not match the staff list cannot register', async ({ page }) => {
  await fillRegistration(page, '9999', 'mary.wrong@ci.paykesho.test');
  await expect(page.locator('body')).toContainText(/does not match|do not match/i);
  await page.goto('/login');
  await page.locator('input[name="email"]').fill('mary.wrong@ci.paykesho.test');
  await page.locator('input[name="password"]').fill('Mary-Test-2026');
  await page.locator('form:has(input[name="password"]) [type="submit"]').first().click();
  await expect(page).toHaveURL(/\/login\?error/);
});

test('an employee on the staff list registers and can sign in', async ({ page }) => {
  await fillRegistration(page, '1002', 'mary@ci.paykesho.test');
  await expect(page.locator('body')).toContainText(/Registration successful/i, { timeout: 30_000 });
  await page.goto('/login');
  await page.locator('input[name="email"]').fill('mary@ci.paykesho.test');
  await page.locator('input[name="password"]').fill('Mary-Test-2026');
  await page.locator('form:has(input[name="password"]) [type="submit"]').first().click();
  await expect(page).toHaveURL(/\/clients\/dashboard$/);
});
