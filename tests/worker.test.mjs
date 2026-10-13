import test, { afterEach } from "node:test";
import assert from "node:assert/strict";
import worker from "../src/index.ts";

const originalFetch = globalThis.fetch;
const origin = "https://paykesho-web.amukenam1.workers.dev";

function makeEnv(assetHandler) {
  return {
    SUPABASE_URL: "https://paykesho-test.supabase.co",
    SUPABASE_ANON_KEY: "test-anon-key",
    ALLOWED_ORIGIN: origin,
    ASSETS: { fetch: assetHandler ?? (async () => new Response("static page")) },
  };
}
function request(path, options = {}) {
  return new Request(origin + path, options);
}
afterEach(() => { globalThis.fetch = originalFetch; });

test("health reports the Worker auth configuration without exposing secrets", async () => {
  const response = await worker.fetch(request("/api/health"), makeEnv());
  const body = await response.json();
  assert.equal(response.status, 200);
  assert.equal(body.runtime, "cloudflare-workers");
  assert.equal(body.springBoot, false);
  assert.equal("SUPABASE_ANON_KEY" in body, false);
});

test("invalid signup payload is rejected before calling Supabase", async () => {
  globalThis.fetch = async () => { throw new Error("unexpected upstream request"); };
  const response = await worker.fetch(request("/api/auth/signup", {
    method: "POST",
    headers: { "content-type": "application/json", origin },
    body: JSON.stringify({ email: "bad", password: "short" }),
  }), makeEnv());
  assert.equal(response.status, 400);
});

test("sign-in sets HttpOnly cookies and never returns tokens to browser JavaScript", async () => {
  globalThis.fetch = async (input) => {
    assert.equal(String(input), "https://paykesho-test.supabase.co/auth/v1/token?grant_type=password");
    return Response.json({
      access_token: "access-token-secret",
      refresh_token: "refresh-token-secret",
      expires_in: 3600,
      user: { id: "user-1", email: "client@example.com" },
    });
  };
  const response = await worker.fetch(request("/api/auth/signin", {
    method: "POST",
    headers: { "content-type": "application/json", origin },
    body: JSON.stringify({ email: "client@example.com", password: "long-enough-password" }),
  }), makeEnv());
  const body = await response.json();
  const cookies = response.headers.getSetCookie().join("\n");
  assert.equal(response.status, 200);
  assert.equal(body.session, true);
  assert.equal(body.access_token, undefined);
  assert.equal(body.refresh_token, undefined);
  assert.match(cookies, /pk_access=.*HttpOnly; Secure; SameSite=Lax/);
  assert.match(cookies, /pk_refresh=.*HttpOnly; Secure; SameSite=Lax/);
  assert.match(response.headers.get("content-security-policy"), /frame-ancestors 'none'/);
});

test("refresh rotates the session from the HttpOnly refresh cookie", async () => {
  globalThis.fetch = async (input, init) => {
    assert.equal(String(input), "https://paykesho-test.supabase.co/auth/v1/token?grant_type=refresh_token");
    assert.equal(JSON.parse(init.body).refresh_token, "old-refresh-token");
    return Response.json({
      access_token: "new-access-token",
      refresh_token: "new-refresh-token",
      expires_in: 1800,
      user: { id: "user-1", email: "client@example.com" },
    });
  };
  const response = await worker.fetch(request("/api/auth/refresh", {
    method: "POST",
    headers: { "content-type": "application/json", origin, cookie: "pk_refresh=old-refresh-token" },
    body: "{}",
  }), makeEnv());
  assert.equal(response.status, 200);
  assert.equal((await response.json()).session, true);
  assert.match(response.headers.getSetCookie().join("\n"), /new-refresh-token/);
});

test("current-user endpoint requires the HttpOnly access cookie", async () => {
  globalThis.fetch = async (_input, init) => {
    assert.equal(init.headers.authorization, "Bearer access-cookie-token");
    return Response.json({ id: "user-1", email: "client@example.com" });
  };
  const unauthorized = await worker.fetch(request("/api/auth/user"), makeEnv());
  assert.equal(unauthorized.status, 401);
  const authorized = await worker.fetch(request("/api/auth/user", {
    headers: { cookie: "pk_access=access-cookie-token" },
  }), makeEnv());
  assert.equal(authorized.status, 200);
  assert.equal((await authorized.json()).id, "user-1");
});

test("financial routes stay disabled until atomic workflows are migrated", async () => {
  const response = await worker.fetch(request("/api/advances"), makeEnv());
  assert.equal(response.status, 501);
  assert.equal((await response.json()).error, "workflow_not_migrated");
});

test("non-API requests are served from Cloudflare static assets", async () => {
  const response = await worker.fetch(request("/"), makeEnv(async () => new Response("PayKesho preview")));
  assert.equal(response.status, 200);
  assert.equal(await response.text(), "PayKesho preview");
});
