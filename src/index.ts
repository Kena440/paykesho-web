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

    if (url.pathname === "/health" || url.pathname === "/api/health") {
      return Response.json({ ok: true, service: "paykesho-web" });
    }

    const container = getContainer(env.PAYKESHO_WEB, "web");
    await container.startAndWaitForPorts({ ports: 8080 });
    return container.fetch(request);
  }
};