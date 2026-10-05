import { Container, getContainer } from "@cloudflare/containers";

export interface Env {
  PAYKESHO_WEB: DurableObjectNamespace<PaykeshoWebContainer>;
  PAYKESHO_DB_URL: string;
  PAYKESHO_DB_USERNAME: string;
  PAYKESHO_DB_PASSWORD: string;
  PAYKESHO_ADMIN_EMAIL: string;
  PAYKESHO_ADMIN_PASSWORD: string;
  NOTIFICATION_SERVICE_URL?: string;
  FILE_SERVICE_URL?: string;
  RATE_LIMITING_ENABLED?: string;
}

export class PaykeshoWebContainer extends Container {
  defaultPort = 8080;
  sleepAfter = "10m";
  pingEndpoint = "ping";

  constructor(ctx: DurableObjectState<{}>, env: Env) {
    super(ctx, env);
    this.envVars = {
      SPRING_PROFILES_ACTIVE: "cloudflare",
      JAVA_OPTS: "-Xmx768m -Xms256m",
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

  override onStart() {
    console.log("PayKesho web container started");
  }

  override onError(error: unknown) {
    console.error("PayKesho web container error", error);
  }

  override onStop(params: { exitCode?: number; reason: string }) {
    console.error("PayKesho web container stopped", params);
  }
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    const container = getContainer(env.PAYKESHO_WEB, "web");

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
      console.error("PayKesho container request failed", error);
      return Response.json(
        {
          ok: false,
          service: "paykesho-web",
          error: "container_request_failed",
          message: error instanceof Error ? error.message : String(error)
        },
        {
          status: 503,
          headers: { "cache-control": "no-store" }
        }
      );
    }
  }
};
