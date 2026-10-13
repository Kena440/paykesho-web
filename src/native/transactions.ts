import { verifyAdminSession, type NativeAuthEnv } from "./admin-auth";
import { supabaseAppRequest } from "./supabase";

const MAX_PAGE_SIZE = 100;
const DEFAULT_PAGE_SIZE = 25;
const SEARCH_PATTERN = /^[\p{L}\p{N} @._'-]{1,80}$/u;
const SETTLEMENT_STATUSES = ["all", "PENDING", "SUCCESSFUL", "FAILED"] as const;

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

/** Read-only administrator transaction listing; all mutations stay on legacy routes for now. */
export async function nativeAdminTransactions(
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
    return json({ ok: false, error: "invalid_pagination" }, 400);
  }

  const status = url.searchParams.get("settlementStatus") || "all";
  if (!(SETTLEMENT_STATUSES as readonly string[]).includes(status)) {
    return json({ ok: false, error: "invalid_settlement_status" }, 400);
  }

  const settled = url.searchParams.get("settled") || "all";
  if (!["all", "true", "false"].includes(settled)) {
    return json({ ok: false, error: "invalid_settled_filter" }, 400);
  }

  const search = (url.searchParams.get("search") || "").trim();
  if (search && !SEARCH_PATTERN.test(search)) {
    return json({ ok: false, error: "invalid_search" }, 400);
  }

  const params = new URLSearchParams({
    select: "id,uuid,reference,client_id,merchant_id,voucher_id,amount,currency,naration,txn_date,settled,settlement_status,settlement_reference,settlement_updated_at",
    order: "txn_date.desc.nullslast",
    limit: String(pageSize),
    offset: String((page - 1) * pageSize)
  });
  if (status !== "all") params.set("settlement_status", `eq.${status}`);
  if (settled === "true") params.set("settled", "eq.true");
  if (settled === "false") params.set("settled", "eq.false");
  if (search) {
    const pattern = `*${search}*`;
    params.set("or", `(reference.ilike.${pattern},naration.ilike.${pattern})`);
  }

  try {
    const response = await supabaseAppRequest(
      env,
      `rest/v1/transactions?${params.toString()}`,
      {
        method: "GET",
        headers: { accept: "application/json", prefer: "count=exact" }
      }
    );

    if (!response.ok) {
      await response.body?.cancel();
      return json({ ok: false, error: "transaction_data_unavailable" }, 503);
    }

    const rows: unknown = await response.json();
    if (!Array.isArray(rows)) {
      return json({ ok: false, error: "invalid_transaction_response" }, 502);
    }

    const contentRange = response.headers.get("content-range");
    const totalText = contentRange?.split("/").at(-1);
    if (!totalText || totalText === "*" || !/^\d+$/.test(totalText)) {
      return json({ ok: false, error: "transaction_count_unavailable" }, 502);
    }

    const total = Number(totalText);
    return json({
      ok: true,
      page,
      pageSize,
      total,
      totalPages: Math.ceil(total / pageSize),
      transactions: rows
    });
  } catch {
    return json({
      ok: false,
      error: "transaction_data_unavailable",
      message: "The transaction directory could not be loaded."
    }, 503);
  }
}
