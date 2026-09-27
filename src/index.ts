import { Container, getContainer } from "@cloudflare/containers";

export interface Env {
  PAYKESHO_WEB: DurableObjectNamespace<PaykeshoWebContainer>;
  PAYKESHO_DB_URL: string;
  PAYKESHO_DB_USERNAME: string;
  PAYKESHO_DB_PASSWORD: string;
  NOTIFICATION_SERVICE_URL?: string;
  FILE_SERVICE_URL?: string;
  RATE_LIMITING_ENABLED?: string;
}

export class PaykeshoWebContainer extends Container {
  defaultPort = 8080;
  sleepAfter = "10m";

  constructor(ctx: DurableObjectState<{}>, env: Env) {
    super(ctx, env);
    this.envVars = {
      SPRING_PROFILES_ACTIVE: "cloudflare",
      JAVA_OPTS: "-Xmx768m -Xms256m",
      PAYKESHO_DB_URL: env.PAYKESHO_DB_URL,
      PAYKESHO_DB_USERNAME: env.PAYKESHO_DB_USERNAME,
      PAYKESHO_DB_PASSWORD: env.PAYKESHO_DB_PASSWORD,
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
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    const container = getContainer(env.PAYKESHO_WEB, "web");

    if (url.pathname === "/health" || url.pathname === "/api/health") {
      try {
        await container.startAndWaitForPorts({
          ports: [8080],
          cancellationOptions: {
            instanceGetTimeoutMS: 15000,
            portReadyTimeoutMS: 120000
          }
        });
        const healthUrl = new URL("/actuator/health", request.url);
        const healthRequest = new Request(healthUrl, request);
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
          { ok: false, service: "paykesho-web", error: "container_unhealthy" },
          { status: 503, headers: { "cache-control": "no-store" } }
        );
      }
    }

    try {
      await container.startAndWaitForPorts({
        ports: [8080],
        cancellationOptions: {
          instanceGetTimeoutMS: 15000,
          portReadyTimeoutMS: 120000
        }
      });
      return container.fetch(request);
    } catch (error) {
      console.error("PayKesho container startup/request failed", error);
      return Response.json(
        {
          ok: false,
          service: "paykesho-web",
          error: "container_startup_failed",
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
