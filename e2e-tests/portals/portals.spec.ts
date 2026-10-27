import { test, expect } from '@playwright/test';
import { portals, signIn, watchServerErrors } from './helpers';

// Portal integration suite: every portal runs behind one origin (the Worker in
// production, scripts/portal-router.mjs in CI) and shares one database.
// Accounts come from e2e-tests/portals/fixtures.sql.

for (const p of portals) {
  test.describe(`${p.name} portal`, () => {
    test('serves its sign-in page under its own path', async ({ page }) => {
      const errors = watchServerErrors(page);
      const res = await page.goto(p.loginPath);
      expect(res?.status()).toBe(200);
      await expect(page.locator('input[name="email"]').first()).toBeVisible();
      await expect(page.locator('input[name="password"]').first()).toBeVisible();
      expect(errors).toEqual([]);
    });

    test('redirects anonymous visitors to its own sign-in page on the public host', async ({ page, baseURL }) => {
      await page.goto(p.base + '/');
      const url = new URL(page.url());
      expect(url.origin).toBe(new URL(baseURL!).origin);
      expect(url.pathname).toBe(p.loginPath);
    });

    test('rejects a wrong password', async ({ page }) => {
      await signIn(page, p, 'definitely-wrong-password');
      await expect(page).toHaveURL((u) => u.pathname === p.loginPath);
      await expect(page.locator('input[name="password"]').first()).toBeVisible();
    });

    test('signs in and loads the dashboard without server errors', async ({ page }) => {
      const errors = watchServerErrors(page);
      await signIn(page, p);
      await expect(page).toHaveURL(p.landing);
      await expect(page.locator('body')).not.toContainText(/Whitelabel Error|Internal Server Error|Exception/);
      await page.waitForTimeout(3000); // let the dashboard cards load
      expect(errors).toEqual([]);
    });
  });
}

test('one browser can be signed in to all portals at once without sessions colliding', async ({ page }) => {
  const errors = watchServerErrors(page);
  for (const p of portals) {
    await signIn(page, p);
    await expect(page, `${p.name} sign-in`).toHaveURL(p.landing);
  }
  // Revisit each portal: every session must still be valid.
  for (const p of portals) {
    await page.goto(p.base + '/');
    expect(new URL(page.url()).pathname, `${p.name} still signed in`).not.toBe(p.loginPath);
  }
  const names = (await page.context().cookies()).map((c) => c.name);
  expect(new Set(names.filter((n) => n.startsWith('PK_')))).toEqual(
    new Set(['PK_CLIENT_SESSION', 'PK_MERCHANT_SESSION', 'PK_EMPLOYER_SESSION', 'PK_ADMIN_SESSION']));
  expect(errors).toEqual([]);
});

test('a session from one portal does not grant access to another', async ({ page }) => {
  await signIn(page, portals[1]); // merchant
  for (const other of [portals[0], portals[2], portals[3]]) {
    await page.goto(other.base + '/');
    expect(new URL(page.url()).pathname, other.name).toBe(other.loginPath);
  }
});

test('sign-in pages link to the other portals', async ({ page }) => {
  for (const p of portals.slice(0, 3)) {
    await page.goto(p.loginPath);
    for (const other of portals.slice(0, 3).filter((o) => o !== p)) {
      await expect(page.locator(`a[href="${other.loginPath}"]`)).toHaveCount(1);
    }
  }
});

test('legacy unauthenticated payment endpoints stay unreachable', async ({ request }) => {
  const res = await request.get('/internal/payment/rest/api/payments');
  expect(res.status()).toBe(404);
});

test('dashboard data endpoints return the signed-in user\'s own data', async ({ page }) => {
  await signIn(page, portals[0]); // client
  const balance = await page.request.get('/api/client/balance');
  expect(balance.status()).toBe(200);
  expect((await balance.json()).formattedBalance).toMatch(/^K \d/);
  const score = await page.request.get('/api/client/credit-score');
  expect(score.status()).toBe(200);

  await signIn(page, portals[1]); // merchant
  const info = await page.request.get('/merchant/api/merchant/info');
  expect(info.status()).toBe(200);
  expect((await info.json()).code).toBe('CIMERCH001');
  const summary = await page.request.get('/merchant/api/transactions/summary');
  expect(summary.status()).toBe(200);
  expect(typeof (await summary.json()).totalTransactions).toBe('number');
});
