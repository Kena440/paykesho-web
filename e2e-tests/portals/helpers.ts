import { Page, Response, expect } from '@playwright/test';

// Shared by the portal specs. Accounts come from e2e-tests/portals/fixtures.sql.
export const PASSWORD = process.env.PORTAL_TEST_PASSWORD || 'Portal-Test-2026!';

export type Portal = {
  name: string;
  base: string;          // context path, '' for the client portal
  loginPath: string;
  email: string;
  landing: RegExp;       // where a successful sign-in lands
};

export const portals: Portal[] = [
  { name: 'client', base: '', loginPath: '/login', email: 'client@ci.paykesho.test', landing: /\/clients\/dashboard$/ },
  { name: 'merchant', base: '/merchant', loginPath: '/merchant/login', email: 'merchant@ci.paykesho.test', landing: /\/merchant\/?$/ },
  { name: 'employer', base: '/employer', loginPath: '/employer/login', email: 'employer@ci.paykesho.test', landing: /\/employer\/?$/ },
  { name: 'back office', base: '/admin', loginPath: '/admin/login', email: 'admin@ci.paykesho.test', landing: /\/admin\/?$/ },
];

export const [clientPortal, merchantPortal, employerPortal, backOffice] = portals;

export function watchServerErrors(page: Page): string[] {
  const failures: string[] = [];
  page.on('response', (r: Response) => {
    const type = r.request().resourceType();
    // Server errors anywhere, and missing endpoints behind the pages' own API calls.
    if (r.status() >= 500 || (r.status() === 404 && (type === 'fetch' || type === 'xhr'))) {
      failures.push(`${r.status()} ${r.request().method()} ${new URL(r.url()).pathname}`);
    }
  });
  page.on('pageerror', (e) => failures.push(`page error: ${e.message}`));
  return failures;
}

export async function signIn(page: Page, p: Portal, password = PASSWORD, email = p.email) {
  await page.goto(p.loginPath);
  await page.locator('input[name="email"]').first().fill(email);
  await page.locator('input[name="password"]').first().fill(password);
  // Dashboards poll in the background, so wait for the sign-in POST and the
  // page it leads to rather than for network idle.
  const posted = page.waitForResponse((r) => r.request().method() === 'POST' && new URL(r.url()).pathname.endsWith('/login'));
  await page.locator('form:has(input[name="password"]) [type="submit"]').first().click();
  await posted;
  await page.waitForLoadState('load');
}

export async function expectNoErrorPage(page: Page) {
  await expect(page.locator('body')).not.toContainText(/Whitelabel Error|Internal Server Error|There was an unexpected error|Exception:/);
}
