import { supabaseAppRequest, type SupabaseWorkerEnv } from "./supabase";

export interface NativeHealthEnv extends SupabaseWorkerEnv {}

function json(body: unknown, status = 200): Response {
  return Response.json(body, {
    status,
    headers: {
      "cache-control": "no-store",
      "x-content-type-options": "nosniff"
    }
  });
}

/** Worker-only liveness probe: does not start or contact a Java container. */
export function nativeLiveness(): Response {
  return json({
    ok: true,
    service: "paykesho-worker-native",
    runtime: "cloudflare-workers",
    route: "worker-native",
    containerRequired: false
  });
}

/**
 * Server-side readiness probe against a harmless existing app-schema table.
 * The response intentionally omits table contents, host details, and secrets.
 */
export async function nativeDatabaseReadiness(
  env: NativeHealthEnv
): Promise<Response> {
  try {
    const response = await supabaseAppRequest(
      env,
      "rest/v1/settings?select=key&limit=1",
      { method: "GET", headers: { accept: "application/json" } }
    );

    if (!response.ok) {
      await response.body?.cancel();
      return json({
        ok: false,
        service: "paykesho-worker-native",
        dependency: "supabase",
        error: "database_readiness_failed",
        upstreamStatus: response.status
      }, 503);
    }

    await response.body?.cancel();
    return json({
      ok: true,
      service: "paykesho-worker-native",
      dependency: "supabase",
      schema: "app"
    });
  } catch (error) {
    const missingConfiguration =
      error instanceof Error && error.message === "Supabase Worker secrets are not configured";
    return json({
      ok: false,
      service: "paykesho-worker-native",
      dependency: "supabase",
      error: missingConfiguration ? "supabase_secrets_missing" : "database_readiness_failed"
    }, 503);
  }
}
