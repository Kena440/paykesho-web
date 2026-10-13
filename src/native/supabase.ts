export interface SupabaseWorkerEnv {
  SUPABASE_URL?: string;
  SUPABASE_SERVICE_ROLE_KEY?: string;
}

/**
 * Make a server-side PostgREST request against the private PayKesho "app"
 * schema. The service-role key must be configured as a Worker secret and must
 * never be returned to a client or bundled into browser assets.
 */
export async function supabaseAppRequest(
  env: SupabaseWorkerEnv,
  path: string,
  init: RequestInit = {}
): Promise<Response> {
  const baseUrl = env.SUPABASE_URL?.trim();
  const serviceKey = env.SUPABASE_SERVICE_ROLE_KEY?.trim();

  if (!baseUrl || !serviceKey) {
    throw new Error("Supabase Worker secrets are not configured");
  }

  let base: URL;
  try {
    base = new URL(baseUrl);
  } catch {
    throw new Error("SUPABASE_URL must be a valid HTTPS URL");
  }

  if (base.protocol !== "https:") {
    throw new Error("SUPABASE_URL must use HTTPS");
  }

  const normalizedPath = path.replace(/^\/+/, "");
  const url = new URL(normalizedPath, base.toString().replace(/\/+$/, "") + "/");
  if (url.origin !== base.origin || !url.pathname.startsWith("/rest/v1/")) {
    throw new Error("Only Supabase REST API paths are permitted");
  }

  const headers = new Headers(init.headers);
  headers.set("apikey", serviceKey);
  headers.set("authorization", `Bearer ${serviceKey}`);
  headers.set("accept-profile", "app");
  if (init.body !== undefined && init.body !== null) {
    headers.set("content-profile", "app");
  }

  return fetch(url, {
    ...init,
    headers,
    redirect: "error"
  });
}
