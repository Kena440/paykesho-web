import { Container, getContainer } from "@cloudflare/containers";

export interface Env {
  PAYKESHO_WEB: DurableObjectNamespace<PaykeshoWebContainer>;
}

export class PaykeshoWebContainer extends Container {
  defaultPort = 8080;
  sleepAfter = "10m";

  envVars = {
    SPRING_PROFILES_ACTIVE: "cloudflare",
    JAVA_OPTS: "-Xmx768m -Xms256m"
  };

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
