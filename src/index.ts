export interface Env {
  SUPABASE_URL: string;
  SUPABASE_ANON_KEY: string;
  ALLOWED_ORIGIN?: string;
  ASSETS: Fetcher;
}

type JsonRecord = Record<string, unknown>;

const json = (data: unknown, status = 200): Response =>
  new Response(JSON.stringify(data), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      "x-content-type-options": "nosniff",
    },
  });

const supabaseBase = (env: Env): string => env.SUPABASE_URL.replace(/\/+$/, "");

const isRecord = (value: unknown): value is JsonRecord =>
  typeof value === "object" && value !== null && !Array.isArray(value);

async function readJson(request: Request): Promise<JsonRecord | null> {
  const contentLength = Number(request.headers.get("content-length") ?? "0");
  if (Number.isFinite(contentLength) && contentLength > 16_384) return null;

  const value: unknown = await request.json().catch(() => null);
  return isRecord(value) ? value : null;
}

async function supabaseAuth(
  env: Env,
  path: string,
  payload: JsonRecord,
): Promise<Response> {
  return fetch(`${supabaseBase(env)}/auth/v1/${path}`, {
    method: "POST",
    headers: {
      "content-type": "application/json",
      apikey: env.SUPABASE_ANON_KEY,
      authorization: `Bearer ${env.SUPABASE_ANON_KEY}`,
    },
    body: JSON.stringify(payload),
  });
}

function upstreamResponse(response: Response): Response {
  return new Response(response.body, {
    status: response.status,
    headers: {
      "content-type": response.headers.get("content-type") ?? "application/json",
      "cache-control": "no-store",
      "x-content-type-options": "nosniff",
    },
  });
}

function cookieValue(request: Request, name: string): string | null {
  const cookieHeader = request.headers.get("cookie");
  if (!cookieHeader) return null;
  for (const part of cookieHeader.split(";")) {
    const separator = part.indexOf("=");
    if (separator < 0 || part.slice(0, separator).trim() !== name) continue;
    try { return decodeURIComponent(part.slice(separator + 1).trim()); }
    catch { return null; }
  }
  return null;
}

function sessionCookie(name: string, value: string, maxAge: number): string {
  return name + "=" + encodeURIComponent(value) + "; Max-Age=" + maxAge + "; Path=/; Secure; HttpOnly; SameSite=Lax";
}

function withSessionCookies(response: Response, values: { accessToken?: string; refreshToken?: string; accessMaxAge?: number; clear?: boolean }): Response {
  const headers = new Headers(response.headers);
  if (values.clear) {
    headers.append("set-cookie", sessionCookie("pk_access", "", 0));
    headers.append("set-cookie", sessionCookie("pk_refresh", "", 0));
  } else {
    if (values.accessToken) headers.append("set-cookie", sessionCookie("pk_access", values.accessToken, values.accessMaxAge ?? 3600));
    if (values.refreshToken) headers.append("set-cookie", sessionCookie("pk_refresh", values.refreshToken, 30 * 24 * 60 * 60));
  }
  return new Response(response.body, { status: response.status, statusText: response.statusText, headers });
}

async function authSessionResponse(response: Response): Promise<Response> {
  const raw = await response.text();
  let payload: unknown = null;
  try { payload = raw ? JSON.parse(raw) as unknown : null; } catch { payload = null; }
  if (!response.ok) return json(isRecord(payload) ? payload : { ok: false, error: "authentication_failed" }, response.status);

  const result = isRecord(payload) ? payload : {};
  const nested = isRecord(result.session) ? result.session : {};
  const accessToken = typeof result.access_token === "string" ? result.access_token : (typeof nested.access_token === "string" ? nested.access_token : undefined);
  const refreshToken = typeof result.refresh_token === "string" ? result.refresh_token : (typeof nested.refresh_token === "string" ? nested.refresh_token : undefined);
  const expiresIn = typeof result.expires_in === "number" ? result.expires_in : (typeof nested.expires_in === "number" ? nested.expires_in : 3600);
  const body = json({ user: result.user ?? nested.user ?? null, session: Boolean(accessToken && refreshToken) }, response.status);
  return withSessionCookies(body, { accessToken, refreshToken, accessMaxAge: Math.min(Math.max(expiresIn, 60), 86400) });
}

function clearSession(response: Response): Response {
  return withSessionCookies(response, { clear: true });
}

function originAllowed(request: Request, env: Env): boolean {
  const origin = request.headers.get("origin");
  if (!origin || !env.ALLOWED_ORIGIN || origin === env.ALLOWED_ORIGIN) return true;
  const requestUrl = new URL(request.url);
  const localHost = requestUrl.hostname === "localhost" || requestUrl.hostname === "127.0.0.1";
  return localHost && origin === requestUrl.origin;
}

async function handleApi(request: Request, env: Env): Promise<Response> {
  const { pathname } = new URL(request.url);

  if (request.method === "OPTIONS") {
    return new Response(null, {
      status: 204,
      headers: {
        "access-control-allow-methods": "GET, POST, OPTIONS",
        "access-control-allow-headers": "authorization, content-type, apikey",
        "access-control-max-age": "600",
      },
    });
  }

  if (pathname === "/api/health" && request.method === "GET") {
    const configured = Boolean(env.SUPABASE_URL && env.SUPABASE_ANON_KEY);
    return json(
      {
        ok: configured,
        service: "paykesho-api",
        runtime: "cloudflare-workers",
        springBoot: false,
        authConfigured: configured,
      },
      configured ? 200 : 503,
    );
  }

  if (pathname === "/api/auth/signup" && request.method === "POST") {
    const body = await readJson(request);
    const email = typeof body?.email === "string" ? body.email.trim().toLowerCase() : "";
    const password = typeof body?.password === "string" ? body.password : "";
    const phoneNumber = typeof body?.phone_number === "string" ? body.phone_number.trim() : "";
    const fullName = typeof body?.full_name === "string" ? body.full_name.trim() : "";

    if (!email || email.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
      || password.length < 8 || password.length > 128) {
      return json({ ok: false, error: "invalid_signup_payload" }, 400);
    }

    const response = await supabaseAuth(env, "signup", {
      email,
      password,
      data: {
        ...(phoneNumber ? { phone_number: phoneNumber } : {}),
        ...(fullName ? { full_name: fullName } : {}),
      },
    });
    return await authSessionResponse(response);
  }

  if (pathname === "/api/auth/signin" && request.method === "POST") {
    const body = await readJson(request);
    const email = typeof body?.email === "string" ? body.email.trim().toLowerCase() : "";
    const password = typeof body?.password === "string" ? body.password : "";

    if (!email || !password || password.length > 128) {
      return json({ ok: false, error: "invalid_signin_payload" }, 400);
    }

    const response = await supabaseAuth(env, "token?grant_type=password", { email, password });
    return await authSessionResponse(response);
  }

  if (pathname === "/api/auth/refresh" && request.method === "POST") {
    if (!originAllowed(request, env)) return json({ ok: false, error: "origin_not_allowed" }, 403);
    const refreshToken = cookieValue(request, "pk_refresh");
    if (!refreshToken) return clearSession(json({ ok: false, error: "session_expired" }, 401));
    const response = await supabaseAuth(env, "token?grant_type=refresh_token", { refresh_token: refreshToken });
    if (response.ok) return await authSessionResponse(response);
    if (response.status === 400 || response.status === 401) return clearSession(json({ ok: false, error: "session_expired" }, 401));
    return json({ ok: false, error: "session_refresh_unavailable" }, 503);
  }

  if (pathname === "/api/auth/password-reset" && request.method === "POST") {
    const body = await readJson(request);
    const email = typeof body?.email === "string" ? body.email.trim().toLowerCase() : "";

    if (!email || email.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return json({ ok: false, error: "invalid_email" }, 400);
    }

    const response = await supabaseAuth(env, "recover", { email });
    if (response.ok || response.status === 400 || response.status === 404) {
      return json({ ok: true, message: "If the account exists, password reset instructions will be sent." });
    }
    return json({ ok: false, error: "password_reset_unavailable" }, 502);
  }

  if (pathname === "/api/auth/signout" && request.method === "POST") {
    if (!originAllowed(request, env)) return json({ ok: false, error: "origin_not_allowed" }, 403);
    const accessToken = cookieValue(request, "pk_access");
    if (accessToken) {
      try {
        await fetch(supabaseBase(env) + "/auth/v1/logout", {
          method: "POST",
          headers: { apikey: env.SUPABASE_ANON_KEY, authorization: "Bearer " + accessToken },
        });
      } catch (error) {
        console.warn("Supabase sign-out could not be confirmed; clearing local session", error);
      }
    }
    return clearSession(new Response(null, { status: 204, headers: { "cache-control": "no-store" } }));
  }

  if (pathname === "/api/auth/user" && request.method === "GET") {
    const accessToken = cookieValue(request, "pk_access");
    if (!accessToken) return json({ ok: false, error: "unauthorized" }, 401);
    const response = await fetch(supabaseBase(env) + "/auth/v1/user", {
      headers: { apikey: env.SUPABASE_ANON_KEY, authorization: "Bearer " + accessToken },
    });
    return upstreamResponse(response);
  }

  const notMigrated = ["/api/advances", "/api/vouchers", "/api/merchant-payments", "/api/ledger"];
  if (notMigrated.some((prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`))) {
    return json({ ok: false, error: "workflow_not_migrated" }, 501);
  }

  return json({ ok: false, error: "not_found" }, 404);
}

function secureHeaders(response: Response, request: Request, env: Env): Response {
  const headers = new Headers(response.headers);
  const origin = request.headers.get("origin");

  if (origin && env.ALLOWED_ORIGIN && origin === env.ALLOWED_ORIGIN) {
    headers.set("access-control-allow-origin", origin);
  }
  headers.set("vary", "Origin");
  headers.set("x-content-type-options", "nosniff");
  headers.set("x-frame-options", "DENY");
  headers.set("referrer-policy", "strict-origin-when-cross-origin");
  headers.set("permissions-policy", "camera=(), microphone=(), geolocation=()");
  headers.set("content-security-policy", "default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'none'; form-action 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'");

  return new Response(response.body, {
    status: response.status,
    statusText: response.statusText,
    headers,
  });
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    try {
      const url = new URL(request.url);
      const response = url.pathname.startsWith("/api/")
        ? await handleApi(request, env)
        : (request.method === "GET" || request.method === "HEAD")
          ? await env.ASSETS.fetch(request)
          : json({ ok: false, error: "method_not_allowed" }, 405);

      return secureHeaders(response, request, env);
    } catch (error) {
      console.error("PayKesho request failed", error);
      return secureHeaders(json({ ok: false, error: "internal_error" }, 500), request, env);
    }
  },
};
