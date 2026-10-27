import { test, expect, Page, APIRequestContext } from '@playwright/test';
import { signIn, portals, Portal } from './helpers';

// Signed in as each portal's user, visit every page reachable from that portal's
// navigation (links only; nothing is submitted) and record anything a tester
// would see as an error: HTTP errors on pages or the calls they make, error
// pages, uncaught script errors, console errors and broken images or scripts.
//
// Runs last (file name), so the pages also show the records created by the
// journey and registration specs.

// Links that change state when followed. They are exercised by the journey spec
// (with the confirmations a user sees) rather than clicked blindly here.
const MUTATING = /\/(logout|delete|remove|disable|enable|approve|reject|adjust|deactivate|activate|toggle|toggle-status|reset-password|block|unlock|cancel|process-settlements|scan-now|fix-uuid|resolve|acknowledge|mark|update-status)(\/|$|\?)/i;
const MAX_PAGES = 120;

type Issue = { page: string; problem: string };

function ownsPath(p: Portal, pathname: string): boolean {
  if (p.base) return pathname === p.base || pathname.startsWith(p.base + '/');
  return !/^\/(admin|merchant|employer|internal)(\/|$)/.test(pathname);
}

function normalise(href: string, pageUrl: string): URL | null {
  try {
    const u = new URL(href, pageUrl);
    if (u.origin !== new URL(pageUrl).origin) return null;
    u.hash = '';
    return u;
  } catch {
    return null;
  }
}

async function contentType(request: APIRequestContext, url: string): Promise<{ status: number; type: string }> {
  const r = await request.get(url, { maxRedirects: 5 });
  return { status: r.status(), type: r.headers()['content-type'] || '' };
}

function watch(page: Page, issues: Issue[], current: () => string) {
  page.on('response', (r) => {
    const url = new URL(r.url());
    if (url.origin !== new URL(page.url() || r.url()).origin) return;
    const type = r.request().resourceType();
    if (r.status() >= 500 || (r.status() >= 400 && r.status() !== 401 && ['fetch', 'xhr', 'script', 'stylesheet', 'image'].includes(type))) {
      issues.push({ page: current(), problem: `${r.status()} ${r.request().method()} ${url.pathname}${url.search} (${type})` });
    }
  });
  page.on('pageerror', (e) => issues.push({ page: current(), problem: `script error: ${e.message.split('\n')[0]}` }));
  page.on('console', (m) => {
    if (m.type() !== 'error') return;
    const text = m.text();
    // Resource failures are reported (with their status) by the response handler.
    if (/Failed to load resource/.test(text)) return;
    issues.push({ page: current(), problem: `console error: ${text.slice(0, 200)}` });
  });
  page.on('requestfailed', (req) => {
    const url = new URL(req.url());
    if (url.origin !== new URL(page.url() || req.url()).origin) return;
    const reason = req.failure()?.errorText || '';
    if (/ERR_ABORTED/.test(reason)) return; // navigation away / cancelled polling
    issues.push({ page: current(), problem: `request failed: ${req.method()} ${url.pathname} ${reason}` });
  });
}

for (const p of portals) {
  test(`${p.name} portal: every page loads without errors`, async ({ page, baseURL }) => {
    test.setTimeout(900_000);
    const origin = new URL(baseURL!).origin;
    const issues: Issue[] = [];
    let current = '';
    watch(page, issues, () => current);

    await signIn(page, p);
    await expect(page).toHaveURL(p.landing);

    const queue: string[] = [new URL(page.url()).pathname];
    const seen = new Set<string>(queue);
    const skipped = new Set<string>();
    const visited: string[] = [];

    while (queue.length && visited.length < MAX_PAGES) {
      const path = queue.shift()!;
      current = path;
      const { status, type } = await contentType(page.request, origin + path);
      if (status >= 400) {
        issues.push({ page: path, problem: `page returns HTTP ${status}` });
        continue;
      }
      if (!type.includes('text/html')) continue; // downloads, images, JSON
      await page.goto(path, { waitUntil: 'load' });
      visited.push(path);
      if (!ownsPath(p, new URL(page.url()).pathname) || new URL(page.url()).pathname === p.loginPath) {
        issues.push({ page: path, problem: `navigation ended at ${new URL(page.url()).pathname} (signed out or wrong portal)` });
        await signIn(page, p);
        continue;
      }
      await page.waitForTimeout(1200); // let dashboard cards and tables load their data
      const body = (await page.locator('body').innerText().catch(() => '')).slice(0, 20000);
      const errorText = body.match(/Whitelabel Error Page|Internal Server Error|There was an unexpected error|Exception:|could not execute|No message available|Error saving|Something went wrong|null pointer|TemplateInputException|SpelEvaluationException/i);
      if (errorText) issues.push({ page: path, problem: `page shows error text: "${errorText[0]}"` });
      const demo = body.match(/Lorem ipsum|Ashley Briggs|Carl Jenkins|Affiliate|John Doe|Restart server 12/);
      if (demo) issues.push({ page: path, problem: `page shows template placeholder content: "${demo[0]}"` });
      const titleText = await page.title();
      if (/error/i.test(titleText)) issues.push({ page: path, problem: `page title is "${titleText}"` });

      const hrefs = await page.$$eval('a[href]', (as) => as.map((a) => a.getAttribute('href') || ''));
      for (const href of hrefs) {
        if (!href || href.startsWith('#') || /^(javascript|mailto|tel):/i.test(href)) continue;
        const u = normalise(href, page.url());
        if (!u || !ownsPath(p, u.pathname)) continue;
        const key = u.pathname + u.search;
        if (MUTATING.test(u.pathname)) { skipped.add(u.pathname); continue; }
        if (seen.has(key)) continue;
        seen.add(key);
        queue.push(key);
      }
    }

    const unique = new Map<string, Issue>();
    for (const i of issues) unique.set(`${i.page} :: ${i.problem}`, i);
    console.log(`[crawl ${p.name}] visited ${visited.length} pages: ${visited.join(' ')}`);
    console.log(`[crawl ${p.name}] state-changing links left to the journey spec: ${[...skipped].join(' ')}`);
    expect([...unique.keys()], `${p.name}: ${visited.length} pages visited`).toEqual([]);
  });
}
