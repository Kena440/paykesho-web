import { test, expect, Page, Response } from '@playwright/test';

// Portal integration suite: every portal runs behind one origin (the Worker in
// production, scripts/portal-router.mjs in CI) and shares one database.
// Accounts come from e2e-tests/portals/fixtures.sql.
const PASSWORD = process.env.PORTAL_TEST_PASSWORD || 'Portal-Test-2026!';

type Portal = {
  name: string;
  base: string;          // context path, '' for the client portal
  loginPath: string;
  email: string;
  landing: RegExp;       // where a successful sign-in lands
};

const portals: Portal[] = [
  { name: 'client', base: '', loginPath: '/login', email: 'client@ci.paykesho.test', landing: /\/clients\/dashboard$/ },
  { name: 'merchant', base: '/merchant', loginPath: '/merchant/login', email: 'merchant@ci.paykesho.test', landing: /\/merchant\/?$/ },
  { name: 'employer', base: '/employer', loginPath: '/employer/login', email: 'employer@ci.paykesho.test', landing: /\/employer\/?$/ },
  { name: 'back office', base: '/admin', loginPath: '/admin/login', email: 'admin@ci.paykesho.test', landing: /\/admin\/?$/ },
];

function watchServerErrors(page: Page): string[] {
  const failures: string[] = [];
  page.on('response', (r: Response) => {
    if (r.status() >= 500) failures.push(`${r.status()} ${r.request().method()} ${new URL(r.url()).pathname}`);
  });
  page.on('pageerror', (e) => failures.push(`page error: ${e.message}`));
  return failures;
}

async function signIn(page: Page, p: Portal, password = PASSWORD) {
  await page.goto(p.loginPath);
  await page.locator('input[name="email"]').first().fill(p.email);
  await page.locator('input[name="password"]').first().fill(password);
  // Dashboards poll in the background, so wait for the post-login navigation
  // (or the error redirect) rather than for network idle.
  await Promise.all([
    page.waitForURL((u) => u.pathname !== p.loginPath || u.search.includes('error'), { waitUntil: 'load' }),
    page.locator('form:has(input[name="password"]) [type="submit"]').first().click(),
  ]);
}

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
      expect(new URL(page.url()).pathname).toBe(p.loginPath);
    });

    test('signs in and loads the dashboard without server errors', async ({ page }) => {
      const errors = watchServerErrors(page);
      await signIn(page, p);
      expect(page.url()).toMatch(p.landing);
      await expect(page.locator('body')).not.toContainText(/Whitelabel Error|Internal Server Error|Exception/);
      expect(errors).toEqual([]);
    });
  });
}

test('one browser can be signed in to all portals at once without sessions colliding', async ({ page }) => {
  const errors = watchServerErrors(page);
  for (const p of portals) {
    await signIn(page, p);
    expect(page.url(), `${p.name} sign-in`).toMatch(p.landing);
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
