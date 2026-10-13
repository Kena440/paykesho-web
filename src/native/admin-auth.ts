import bcrypt from "bcryptjs";
import { supabaseAppRequest, type SupabaseWorkerEnv } from "./supabase";

export interface NativeAuthEnv extends SupabaseWorkerEnv {
  PAYKESHO_ADMIN_EMAIL?: string;
  PAYKESHO_ADMIN_PASSWORD?: string;
  PAYKESHO_SESSION_SECRET?: string;
}

const SESSION_COOKIE = "__Host-paykesho_admin";
const SESSION_SECONDS = 8 * 60 * 60;

type AdminSession = {
  sub: string;
  role: "ADMIN";
  iat: number;
  exp: number;
};

function json(body: unknown, status = 200, headers?: HeadersInit): Response {
  const responseHeaders = new Headers(headers);
  responseHeaders.set("content-type", "application/json; charset=utf-8");
  responseHeaders.set("cache-control", "no-store");
  responseHeaders.set("x-content-type-options", "nosniff");
  responseHeaders.set("referrer-policy", "no-referrer");
  return Response.json(body, { status, headers: responseHeaders });
}

function base64UrlEncode(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
}

function base64UrlDecode(value: string): Uint8Array {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized + "=".repeat((4 - normalized.length % 4) % 4);
  const binary = atob(padded);
  return Uint8Array.from(binary, (char) => char.charCodeAt(0));
}

function toArrayBuffer(bytes: Uint8Array): ArrayBuffer {
  const copy = new Uint8Array(bytes.byteLength);
  copy.set(bytes);
  return copy.buffer;
}

function constantTimeEqual(left: string, right: string): boolean {
  const a = new TextEncoder().encode(left);
  const b = new TextEncoder().encode(right);
  let difference = a.length ^ b.length;
  const length = Math.max(a.length, b.length);
  for (let i = 0; i < length; i++) {
    difference |= (a[i % Math.max(a.length, 1)] ?? 0) ^ (b[i % Math.max(b.length, 1)] ?? 0);
  }
  return difference === 0;
}

async function signingKey(env: NativeAuthEnv): Promise<CryptoKey | null> {
  const secret = env.PAYKESHO_SESSION_SECRET?.trim();
  if (!secret || secret.length < 32) return null;
  return crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign", "verify"]
  );
}

async function issueSession(env: NativeAuthEnv, email: string): Promise<string | null> {
  const key = await signingKey(env);
  if (!key) return null;
  const now = Math.floor(Date.now() / 1000);
  const payload: AdminSession = {
    sub: email.toLowerCase(),
    role: "ADMIN",
    iat: now,
    exp: now + SESSION_SECONDS
  };
  const encodedPayload = base64UrlEncode(new TextEncoder().encode(JSON.stringify(payload)));
  const signature = await crypto.subtle.sign(
    "HMAC",
    key,
    new TextEncoder().encode(encodedPayload)
  );
  return encodedPayload + "." + base64UrlEncode(new Uint8Array(signature));
}

export async function verifyAdminSession(
  request: Request,
  env: NativeAuthEnv
): Promise<AdminSession | null> {
  const cookieHeader = request.headers.get("cookie") || "";
  const token = cookieHeader
    .split(";")
    .map((part) => part.trim())
    .find((part) => part.startsWith(SESSION_COOKIE + "="))
    ?.slice(SESSION_COOKIE.length + 1);
  if (!token) return null;

  const parts = token.split(".");
  if (parts.length !== 2) return null;
  const key = await signingKey(env);
  if (!key) return null;

  try {
    const valid = await crypto.subtle.verify(
      "HMAC",
      key,
      toArrayBuffer(base64UrlDecode(parts[1])),
      new TextEncoder().encode(parts[0])
    );
    if (!valid) return null;

    const session = JSON.parse(new TextDecoder().decode(base64UrlDecode(parts[0]))) as AdminSession;
    const now = Math.floor(Date.now() / 1000);
    if (
      !session ||
      typeof session.sub !== "string" ||
      session.role !== "ADMIN" ||
      !Number.isInteger(session.iat) ||
      !Number.isInteger(session.exp) ||
      session.iat > now + 60 ||
      session.exp <= now ||
      session.exp - session.iat > SESSION_SECONDS
    ) return null;
    return session;
  } catch {
    return null;
  }
}

function cookie(token: string, maxAge: number): string {
  return `${SESSION_COOKIE}=${token}; Path=/; Max-Age=${maxAge}; Secure; HttpOnly; SameSite=Strict`;
}

function sameOrigin(request: Request): boolean {
  const origin = request.headers.get("origin");
  if (!origin) return true;
  try {
    return new URL(origin).origin === new URL(request.url).origin;
  } catch {
    return false;
  }
}

type DatabaseAdmin = {
  id: number;
  email: string;
  password: string;
  enabled: boolean;
  deleted: boolean;
  account_non_locked: boolean;
};

const ADMIN_ROLE_NAMES = new Set(["admin", "superadmin", "role_admin", "role_superadmin"]);

async function authenticateDatabaseAdmin(email: string, password: string, env: NativeAuthEnv): Promise<DatabaseAdmin | null> {
  const userParams = new URLSearchParams({
    select: "id,email,password,enabled,deleted,account_non_locked",
    email: `eq.${email}`,
    limit: "1"
  });
  const userResponse = await supabaseAppRequest(env, `rest/v1/users?${userParams.toString()}`, {
    method: "GET",
    headers: { accept: "application/json" }
  });
  if (!userResponse.ok) {
    await userResponse.body?.cancel();
    throw new Error("Administrator lookup failed");
  }

  const userRows: unknown = await userResponse.json();
  if (!Array.isArray(userRows) || userRows.length === 0) return null;
  const user = userRows[0] as Partial<DatabaseAdmin>;
  if (
    !Number.isSafeInteger(user.id) ||
    typeof user.email !== "string" ||
    typeof user.password !== "string" ||
    user.enabled !== true ||
    user.deleted !== false ||
    user.account_non_locked !== true
  ) return null;

  const rolesParams = new URLSearchParams({ select: "role_id", user_id: `eq.${user.id}` });
  const membershipResponse = await supabaseAppRequest(env, `rest/v1/user_roles?${rolesParams.toString()}`, {
    method: "GET",
    headers: { accept: "application/json" }
  });
  if (!membershipResponse.ok) {
    await membershipResponse.body?.cancel();
    throw new Error("Administrator role lookup failed");
  }
  const membershipRows: unknown = await membershipResponse.json();
  if (!Array.isArray(membershipRows)) throw new Error("Invalid administrator role response");

  const roleIds = membershipRows
    .map((row) => (row && typeof row === "object" ? (row as { role_id?: unknown }).role_id : null))
    .filter((id): id is number => typeof id === "number" && Number.isSafeInteger(id) && id > 0);
  if (roleIds.length === 0) return null;

  const rolesParams = new URLSearchParams({
    select: "name,deleted",
    id: `in.(${[...new Set(roleIds)].join(",")})`
  });
  const rolesResponse = await supabaseAppRequest(env, `rest/v1/role?${rolesParams.toString()}`, {
    method: "GET",
    headers: { accept: "application/json" }
  });
  if (!rolesResponse.ok) {
    await rolesResponse.body?.cancel();
    throw new Error("Administrator role definitions could not be loaded");
  }
  const roleRows: unknown = await rolesResponse.json();
  if (!Array.isArray(roleRows)) throw new Error("Invalid administrator role definitions");

  const isAdmin = roleRows.some((row) => {
    if (!row || typeof row !== "object") return false;
    const role = row as { name?: unknown; deleted?: unknown };
    return role.deleted !== true &&
      typeof role.name === "string" &&
      ADMIN_ROLE_NAMES.has(role.name.trim().toLowerCase());
  });
  if (!isAdmin) return null;

  const matches = await bcrypt.compare(password, user.password);
  return matches ? user as DatabaseAdmin : null;
}

/**
 * Initial Worker-native administrator session slice.
 * This intentionally supports only the configured bootstrap administrator;
 * database-backed admin accounts must be migrated before replacing /admin.
 */
export async function nativeAdminLogin(request: Request, env: NativeAuthEnv): Promise<Response> {
  if (request.method !== "POST") {
    return json({ ok: false, error: "method_not_allowed" }, 405, { allow: "POST" });
  }
  if (!sameOrigin(request)) return json({ ok: false, error: "invalid_origin" }, 403);
  if (!request.headers.get("content-type")?.toLowerCase().includes("application/json")) {
    return json({ ok: false, error: "content_type_must_be_json" }, 415);
  }

  const configuredEmail = env.PAYKESHO_ADMIN_EMAIL?.trim();
  const configuredPassword = env.PAYKESHO_ADMIN_PASSWORD;
  if (!(await signingKey(env))) {
    return json({ ok: false, error: "admin_auth_not_configured" }, 503);
  }

  let body: unknown;
  try {
    const raw = await request.text();
    if (new TextEncoder().encode(raw).byteLength > 8192) {
      return json({ ok: false, error: "request_too_large" }, 413);
    }
    body = JSON.parse(raw);
  } catch {
    return json({ ok: false, error: "invalid_json" }, 400);
  }

  const input = body as { email?: unknown; password?: unknown } | null;
  const email = typeof input?.email === "string" ? input.email.trim() : "";
  const password = typeof input?.password === "string" ? input.password : "";
  if (
    !email ||
    email.length > 128 ||
    !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) ||
    !password ||
    password.length > 1024
  ) {
    return json({ ok: false, error: "invalid_credentials" }, 401);
  }

  let authenticatedEmail: string | null = null;
  if (configuredEmail && configuredPassword && email.toLowerCase() === configuredEmail.toLowerCase()) {
    // Keep the bootstrap administrator's precedence consistent with Spring.
    if (
      constantTimeEqual(email.toLowerCase(), configuredEmail.toLowerCase()) &&
      constantTimeEqual(password, configuredPassword)
    ) {
      authenticatedEmail = configuredEmail;
    } else {
      return json({ ok: false, error: "invalid_credentials" }, 401);
    }
  } else {
    try {
      const databaseAdmin = await authenticateDatabaseAdmin(email, password, env);
      if (databaseAdmin) authenticatedEmail = databaseAdmin.email;
    } catch {
      return json({ ok: false, error: "admin_auth_unavailable" }, 503);
    }
    if (!authenticatedEmail) {
      return json({ ok: false, error: "invalid_credentials" }, 401);
    }
  }

  const token = await issueSession(env, authenticatedEmail);
  if (!token) return json({ ok: false, error: "admin_auth_not_configured" }, 503);
  return json(
    { ok: true, user: { email: authenticatedEmail, role: "ADMIN" }, expiresIn: SESSION_SECONDS },
    200,
    { "set-cookie": cookie(token, SESSION_SECONDS) }
  );
}

export async function nativeAdminSession(request: Request, env: NativeAuthEnv): Promise<Response> {
  if (request.method !== "GET") {
    return json({ ok: false, error: "method_not_allowed" }, 405, { allow: "GET" });
  }
  const session = await verifyAdminSession(request, env);
  if (!session) return json({ ok: false, authenticated: false }, 401);
  return json({
    ok: true,
    authenticated: true,
    user: { email: session.sub, role: session.role },
    expiresAt: new Date(session.exp * 1000).toISOString()
  });
}

export function nativeAdminLogout(request: Request): Response {
  if (request.method !== "POST") {
    return json({ ok: false, error: "method_not_allowed" }, 405, { allow: "POST" });
  }
  if (!sameOrigin(request)) return json({ ok: false, error: "invalid_origin" }, 403);
  return json({ ok: true }, 200, { "set-cookie": cookie("", 0) });
}
