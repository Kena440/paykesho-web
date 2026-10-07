// PayKesho production deployment is managed by Cloudflare Workers Builds.
import { Container, getContainer } from "@cloudflare/containers";

export interface Env {
  PAYKESHO_WEB: DurableObjectNamespace<PaykeshoWebContainer>;
  PAYKESHO_CLIENT: DurableObjectNamespace<PaykeshoClientContainer>;
  PAYKESHO_PAYMENT: DurableObjectNamespace<PaykeshoPaymentContainer>;
  PAYKESHO_DB_URL: string;
  PAYKESHO_DB_USERNAME: string;
  PAYKESHO_DB_PASSWORD: string;
  PAYKESHO_ADMIN_EMAIL: string;
  PAYKESHO_ADMIN_PASSWORD: string;
  NOTIFICATION_SERVICE_URL?: string;
  FILE_SERVICE_URL?: string;
  RATE_LIMITING_ENABLED?: string;
  LENCO_API_BASE_URL?: string;
  LENCO_API_TOKEN: string;
  LENCO_ACCOUNT_ID: string;
  PAYKESHO_INTERNAL_TOKEN: string;
  PAYMENT_SERVICE_URL?: string;
}

function paykeshoEnv(env: Env, profile: string): Record<string, string> {
  return {
    SPRING_PROFILES_ACTIVE: profile,
    JAVA_OPTS: "-Xmx512m -Xms128m -XX:MaxMetaspaceSize=192m",
    PAYKESHO_DB_URL: env.PAYKESHO_DB_URL,
    PAYKESHO_DB_USERNAME: env.PAYKESHO_DB_USERNAME,
    PAYKESHO_DB_PASSWORD: env.PAYKESHO_DB_PASSWORD,
    NOTIFICATION_SERVICE_URL: env.NOTIFICATION_SERVICE_URL || "http://localhost:8085",
    FILE_SERVICE_URL: env.FILE_SERVICE_URL || "http://localhost:8086",
    RATE_LIMITING_ENABLED: env.RATE_LIMITING_ENABLED || "true",
    PAYKESHO_INTERNAL_TOKEN: env.PAYKESHO_INTERNAL_TOKEN || "",
    PAYMENT_SERVICE_URL: env.PAYMENT_SERVICE_URL || "http://localhost:8787/internal/payment"
  };
}

function backofficeEnv(env: Env): Record<string, string> {
  return {
    ...paykeshoEnv(env, "cloudflare"),
    PAYKESHO_ADMIN_EMAIL: env.PAYKESHO_ADMIN_EMAIL,
    PAYKESHO_ADMIN_PASSWORD: env.PAYKESHO_ADMIN_PASSWORD
  };
}

function paymentEnv(env: Env): Record<string, string> {
  return {
    ...paykeshoEnv(env, "cloudflare"),
    LENCO_API_BASE_URL: env.LENCO_API_BASE_URL || "https://api.lenco.co/access/v2",
    LENCO_API_TOKEN: env.LENCO_API_TOKEN || "",
    LENCO_ACCOUNT_ID: env.LENCO_ACCOUNT_ID || ""
  };
}

export class PaykeshoWebContainer extends Container {
  defaultPort = 8080;
  sleepAfter = "10m";

  constructor(ctx: DurableObjectState<{}>, env: Env) {
    super(ctx, env);
    this.envVars = backofficeEnv(env);
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

export class PaykeshoPaymentContainer extends Container {
  defaultPort = 8084;
  sleepAfter = "10m";

  constructor(ctx: DurableObjectState<{}>, env: Env) {
    super(ctx, env);
    this.envVars = paymentEnv(env);
  }

  override onStart() {
    console.log("PayKesho payment container started");
  }

  override onError(error: unknown) {
    console.error("PayKesho payment container error", error);
  }

  override onStop(params: { exitCode?: number; reason: string }) {
    console.error("PayKesho payment container stopped", params);
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

async function forwardToContainer(
  container: PaykeshoWebContainer | PaykeshoClientContainer | PaykeshoPaymentContainer,
  request: Request,
  port: number
): Promise<Response> {
  // Explicitly wait for readiness. This avoids routing a request into an instance
  // that is still starting (or racing another request that is starting it).
  await container.startAndWaitForPorts({
    ports: [port],
    cancellationOptions: {
      portReadyTimeoutMS: 60_000
    }
  });
  return await container.fetch(request);
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
        adminPasswordPresent: Boolean(env.PAYKESHO_ADMIN_PASSWORD)
      }, { headers: { "cache-control": "no-store" } });
    }

    const isPayment = url.pathname === "/webhooks/lenco"
      || url.pathname.startsWith("/internal/payment/");
    const isAdmin = url.pathname === "/admin" || url.pathname.startsWith("/admin/");

    const namespace = isPayment
      ? env.PAYKESHO_PAYMENT
      : (isAdmin ? env.PAYKESHO_WEB : env.PAYKESHO_CLIENT);
    const containerId = isPayment ? "payment" : (isAdmin ? "backoffice" : "client");
    const container = getContainer(namespace, containerId);

    try {
      if (isPayment && url.pathname.startsWith("/internal/payment/")) {
        const rewritten = new URL(request.url);
        rewritten.pathname = url.pathname.replace(/^\/internal\/payment/, "") || "/";
        return await forwardToContainer(
          container,
          new Request(rewritten.toString(), request),
          8084
        );
      }

      return await forwardToContainer(container, request, 8080);
    } catch (error) {
      console.error(`PayKesho ${containerId} container request failed`, error);
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
          error: "container_request_failed",
          message: error instanceof Error ? error.message : String(error),
          state
        },
        { status: 503, headers: { "cache-control": "no-store" } }
      );
    }
  }
};
