import { verifyAdminSession, type NativeAuthEnv } from "./admin-auth";
import { supabaseAppRequest } from "./supabase";

const DASHBOARD_COUNTS = [
  ["organizations", "organizations", ""],
  ["merchants", "merchants", ""],
  ["clients", "clients", ""],
  ["transactions", "transactions", ""],
  ["vouchers", "vouchers", ""],
  ["activeOrganizations", "organizations", "enabled=eq.true"],
  ["inactiveOrganizations", "organizations", "enabled=eq.false"],
  ["activeMerchants", "merchants", "enabled=eq.true"],
  ["inactiveMerchants", "merchants", "enabled=eq.false"],
  ["activeClients", "clients", "enabled=eq.true"],
  ["inactiveClients", "clients", "enabled=eq.false"]
] as const;

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

async function countTable(
  env: NativeAuthEnv,
  table: string,
  filters = ""
): Promise<number> {
  const response = await supabaseAppRequest(
    env,
    `rest/v1/${table}?select=id${filters ? "&" + filters : ""}&limit=0`,
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
    throw new Error(`Dashboard count query failed for ${table} (HTTP ${response.status})`);
  }

  await response.body?.cancel();
  const contentRange = response.headers.get("content-range");
  const total = contentRange?.split("/").at(-1);
  if (!total || total === "*" || !/^\d+$/.test(total)) {
    throw new Error(`Dashboard count unavailable for ${table}`);
  }
  return Number(total);
}

/**
 * Read-only first dashboard slice. Only a signed native administrator session
 * can access it. No record data or financial balances are returned.
 */
export async function nativeAdminDashboard(
  request: Request,
  env: NativeAuthEnv
): Promise<Response> {
  if (request.method !== "GET") {
    return json({ ok: false, error: "method_not_allowed" }, 405);
  }

  const session = await verifyAdminSession(request, env);
  if (!session) return json({ ok: false, error: "unauthorized" }, 401);

  try {
    const entries = await Promise.all(
      DASHBOARD_COUNTS.map(async ([metric, table, filters]) => [
        metric,
        await countTable(env, table, filters)
      ] as const)
    );

    return json({
      ok: true,
      generatedAt: new Date().toISOString(),
      metrics: Object.fromEntries(entries)
    });
  } catch {
    return json({
      ok: false,
      error: "dashboard_data_unavailable",
      message: "The dashboard could not read the required PayKesho data."
    }, 503);
  }
}
