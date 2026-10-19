import { verifyAdminSession, type NativeAuthEnv } from "./admin-auth";
import { supabaseAppRequest } from "./supabase";

const MAX_PAGE_SIZE = 100;
const DEFAULT_PAGE_SIZE = 25;
const SEARCH_PATTERN = /^[\p{L}\p{N} @._'-]{1,80}$/u;

function json(body: unknown, status = 200): Response {
  return Response.json(body, {
    status,
    headers: {
      "cache-control": "no-store",
      "content-type": "application/json; charset=utf-8",
      "x-content-type-options": "nosniff",
      "referrer-policy": "no-referrer"
    }
  });
}

function parseBoundedInteger(value: string | null, fallback: number, min: number, max: number): number | null {
  if (value === null || value.trim() === "") return fallback;
  if (!/^\d+$/.test(value)) return null;
  const parsed = Number(value);
  if (!Number.isSafeInteger(parsed) || parsed < min || parsed > max) return null;
  return parsed;
}

/**
 * Read-only paginated administrator view of clients. Deliberately excludes
 * credentials, NRC, TPIN, password-reset tokens, verification codes and balances.
 */
export async function nativeAdminClients(
  request: Request,
  env: NativeAuthEnv
): Promise<Response> {
  if (request.method !== "GET") {
    return json({ ok: false, error: "method_not_allowed" }, 405);
  }

  const session = await verifyAdminSession(request, env);
  if (!session) return json({ ok: false, error: "unauthorized" }, 401);

  const url = new URL(request.url);
  const page = parseBoundedInteger(url.searchParams.get("page"), 1, 1, 1000000);
  const pageSize = parseBoundedInteger(url.searchParams.get("pageSize"), DEFAULT_PAGE_SIZE, 1, MAX_PAGE_SIZE);
  if (page === null || pageSize === null) {
    return json({
      ok: false,
      error: "invalid_pagination",
      message: `page must be 1 or greater and pageSize must be between 1 and ${MAX_PAGE_SIZE}.`
    }, 400);
  }

  const enabled = url.searchParams.get("status") || "all";
  if (!["all", "active", "inactive"].includes(enabled)) {
    return json({ ok: false, error: "invalid_status" }, 400);
  }

  const search = (url.searchParams.get("search") || "").trim();
  if (search && !SEARCH_PATTERN.test(search)) {
    return json({ ok: false, error: "invalid_search" }, 400);
  }

  const params = new URLSearchParams({
    select: "id,uuid,code,name,email,phone_number,enabled,created_time,organization_id",
    order: "created_time.desc.nullslast",
    limit: String(pageSize),
    offset: String((page - 1) * pageSize)
  });
  if (enabled === "active") params.set("enabled", "eq.true");
  if (enabled === "inactive") params.set("enabled", "eq.false");
  if (search) {
    const pattern = `*${search}*`;
    params.set("or", `(name.ilike.${pattern},email.ilike.${pattern},code.ilike.${pattern})`);
  }

  try {
    const response = await supabaseAppRequest(
      env,
      `rest/v1/clients?${params.toString()}`,
      {
        method: "GET",
        headers: {
          accept: "application/json",
          prefer: "count=exact"
        }
      }
    );

    if (!response.ok) {
      await response.body?.cancel();
      return json({
        ok: false,
        error: "client_data_unavailable",
        message: "The client directory could not be loaded."
      }, 503);
    }

    const rows: unknown = await response.json();
    if (!Array.isArray(rows)) {
      return json({ ok: false, error: "invalid_client_response" }, 502);
    }

    const contentRange = response.headers.get("content-range");
    const totalText = contentRange?.split("/").at(-1);
    if (!totalText || totalText === "*" || !/^\d+$/.test(totalText)) {
      return json({ ok: false, error: "client_count_unavailable" }, 502);
    }

    const total = Number(totalText);
    return json({
      ok: true,
      page,
      pageSize,
      total,
      totalPages: Math.ceil(total / pageSize),
      clients: rows
    });
  } catch {
    return json({
      ok: false,
      error: "client_data_unavailable",
      message: "The client directory could not be loaded."
    }, 503);
  }
}
