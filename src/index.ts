import { Container, getContainer } from "@cloudflare/containers";

export interface Env {
  PAYKESHO_WEB: DurableObjectNamespace<PaykeshoWebContainer>;
  SPRING_DATASOURCE_URL: string;
  SPRING_DATASOURCE_USERNAME: string;
  SPRING_DATASOURCE_PASSWORD: string;
  GOOGLE_CLIENT_ID: string;
  GOOGLE_CLIENT_SECRET: string;
  FACEBOOK_CLIENT_ID: string;
  FACEBOOK_CLIENT_SECRET: string;
  NOTIFICATION_SERVICE_URL?: string;
  FILE_SERVICE_URL?: string;
  RATE_LIMITING_ENABLED?: string;
}

export class PaykeshoWebContainer extends Container {
  defaultPort = 8080;
  sleepAfter = "10m";

  constructor(ctx: DurableObjectState, env: Env) {
    super(ctx, env);
    this.envVars = {
      SPRING_PROFILES_ACTIVE: "cloudflare",
      JAVA_OPTS: "-Xmx768m -Xms256m",
      SPRING_DATASOURCE_URL: env.SPRING_DATASOURCE_URL,
      SPRING_DATASOURCE_USERNAME: env.SPRING_DATASOURCE_USERNAME,
      SPRING_DATASOURCE_PASSWORD: env.SPRING_DATASOURCE_PASSWORD,
      GOOGLE_CLIENT_ID: env.GOOGLE_CLIENT_ID,
      GOOGLE_CLIENT_SECRET: env.GOOGLE_CLIENT_SECRET,
      FACEBOOK_CLIENT_ID: env.FACEBOOK_CLIENT_ID,
      FACEBOOK_CLIENT_SECRET: env.FACEBOOK_CLIENT_SECRET,
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
    return container.fetch(request);
  }
};
