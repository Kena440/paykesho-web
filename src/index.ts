import { Container, getContainer } from "@cloudflare/containers";

export interface Env {
  PAYKESHO_WEB: DurableObjectNamespace<PaykeshoWebContainer>;
  PAYKESHO_CLIENT: DurableObjectNamespace<PaykeshoClientContainer>;
  PAYKESHO_DB_URL: string;
  PAYKESHO_DB_USERNAME: string;
  PAYKESHO_DB_PASSWORD: string;
  PAYKESHO_ADMIN_EMAIL: string;
  PAYKESHO_ADMIN_PASSWORD: string;
  NOTIFICATION_SERVICE_URL?: string;
  FILE_SERVICE_URL?: string;
  RATE_LIMITING_ENABLED?: string;
}

function paykeshoEnv(env: Env, profile: string): Record<string, string> {
  return {
    SPRING_PROFILES_ACTIVE: profile,
    JAVA_OPTS: "-Xmx512m -Xms128m -XX:MaxMetaspaceSize=192m",
    PAYKESHO_DB_URL: env.PAYKESHO_DB_URL,
    PAYKESHO_DB_USERNAME: env.PAYKESHO_DB_USERNAME,
    PAYKESHO_DB_PASSWORD: env.PAYKESHO_DB_PASSWORD,
    PAYKESHO_ADMIN_EMAIL: env.PAYKESHO_ADMIN_EMAIL,
    PAYKESHO_ADMIN_PASSWORD: env.PAYKESHO_ADMIN_PASSWORD,
    NOTIFICATION_SERVICE_URL: env.NOTIFICATION_SERVICE_URL || "http://localhost:8085",
    FILE_SERVICE_URL: env.FILE_SERVICE_URL || "http://localhost:8086",
    RATE_LIMITING_ENABLED: env.RATE_LIMITING_ENABLED || "true"
  };
}

export class PaykeshoWebContainer extends Container {
  defaultPort = 8080;
  sleepAfter = "10m";

  constructor(ctx: DurableObjectState<{}>, env: Env) {
    super(ctx, env);
    this.envVars = paykeshoEnv(env, "cloudflare");
  }

  override onStart() {
    console.log("PayKesho back-office container started");
  }

  override onError(error: unknown) {
    console.error("PayKesho back-office container error", error);
  }

  override onStop(params: { exitCode?: number; reason: string }) {
    console.error("PayKesho back-office container stopped", params);
  }
}

export class PaykeshoClientContainer extends Container {
  defaultPort = 8080;
  sleepAfter = "10m";

  constructor(ctx: DurableObjectState<{}>, env: Env) {
    super(ctx, env);
    this.envVars = paykeshoEnv(env, "cloudflare");
  }

  override onStart() {
    console.log("PayKesho customer container started");
  }

  override onError(error: unknown) {
    console.error("PayKesho customer container error", error);
  }

  override onStop(params: { exitCode?: number; reason: string }) {
    console.error("PayKesho customer container stopped", params);
  }
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);

    if (url.pathname === "/debug/config") {
      const dbUrl = env.PAYKESHO_DB_URL || "";
      return Response.json({
        dbUrlPresent: Boolean(dbUrl),
        dbUrlJdbcPostgres: dbUrl.startsWith("jdbc:postgresql://"),
        dbHostPresent: Boolean(dbUrl.match(/^jdbc:postgresql:\/\/([^/?]+)/)),
        dbUsernamePresent: Boolean(env.PAYKESHO_DB_USERNAME),
        dbPasswordPresent: Boolean(env.PAYKESHO_DB_PASSWORD),
        adminEmailPresent: Boolean(env.PAYKESHO_ADMIN_EMAIL),
        adminPasswordPresent: Boolean(env.PAYKESHO_ADMIN_PASSWORD),
        adminEmail: env.PAYKESHO_ADMIN_EMAIL || null
      }, { headers: { "cache-control": "no-store" } });
    }

    const isAdmin = url.pathname === "/admin" || url.pathname.startsWith("/admin/");
    const namespace = isAdmin ? env.PAYKESHO_WEB : env.PAYKESHO_CLIENT;
    const containerId = isAdmin ? "backoffice" : "client";
    const container = getContainer(namespace, containerId);

    try {
      await container.start();
    } catch (error) {
      console.error(`PayKesho ${containerId} container start failed`, error);
      let state: unknown = null;
      try {
        state = await container.getState();
      } catch (stateError) {
        console.error("PayKesho container state lookup failed", stateError);
      }
      return Response.json(
        {
          ok: false,
          service: `paykesho-${containerId}`,
          error: "container_start_failed",
          message: error instanceof Error ? error.message : String(error),
          state
        },
        { status: 503, headers: { "cache-control": "no-store" } }
      );
    }

    if (url.pathname === "/health" || url.pathname === "/api/health") {
      try {
        const healthRequest = new Request(new URL("/actuator/health", request.url), request);
        const response = await container.fetch(healthRequest);

        return new Response(response.body, {
          status: response.status,
          headers: {
            "content-type": "application/json; charset=utf-8",
            "cache-control": "no-store"
          }
        });
      } catch (error) {
        console.error("PayKesho health check failed", error);
        return Response.json(
          {
            ok: false,
            service: "paykesho-web",
            error: "container_unhealthy",
            message: error instanceof Error ? error.message : String(error)
          },
          { status: 503, headers: { "cache-control": "no-store" } }
        );
      }
    }

    try {
      return await container.fetch(request);
    } catch (error) {
      console.error(`PayKesho ${containerId} request failed`, error);
      return Response.json(
        {
          ok: false,
          service: `paykesho-${containerId}`,
          error: "container_request_failed",
          message: error instanceof Error ? error.message : String(error)
        },
        { status: 503, headers: { "cache-control": "no-store" } }
      );
    }
  }
};
