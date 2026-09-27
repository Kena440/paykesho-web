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

  override onStop(params: { exitCode?: number; reason: string }) {
    console.error("PayKesho web container stopped", params);
  }
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    const container = getContainer(env.PAYKESHO_WEB, "web");

    // Start the container explicitly, but do not make the Worker-side startup
    // health probe depend on Spring Boot Actuator/DB readiness. The first
    // container.fetch() will wait for the application port as part of the
    // normal Container request lifecycle.
    try {
      await container.start();
    } catch (error) {
      console.error("PayKesho container start failed", error);
      let state: unknown = null;
      try {
        state = await container.getState();
      } catch (stateError) {
        console.error("PayKesho container state lookup failed", stateError);
      }
      return Response.json(
        {
          ok: false,
          service: "paykesho-web",
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
