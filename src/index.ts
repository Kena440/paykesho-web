// PayKesho production deployment is managed by Cloudflare Workers Builds.
import { DurableObject } from "cloudflare:workers";

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

function rewriteRequestForContainer(request: Request): Request {
  const url = new URL(request.url);
  url.protocol = "http:";
  url.host = "container";
  const forwarded = new Request(url, request);
  forwarded.headers.delete("host");
  return forwarded;
}

async function waitForContainerPort(
  container: NonNullable<DurableObjectState["container"]>,
  portNumber: number,
  healthPath: string,
  envVars: Record<string, string>
): Promise<void> {
  await container.setInactivityTimeout(10 * 60 * 1000);

  let containerFailure: unknown;

  if (!container.running) {
    container.start({ env: envVars, enableInternet: true });
    void container.monitor().catch((error) => {
      containerFailure = error;
      console.error("PayKesho container monitor error", error);
    });
  }

  const port = container.getTcpPort(portNumber);
  let lastError: unknown;

  // Cloudflare's direct Container API returns from start() before the
  // application is ready. Poll the real Spring Boot health endpoint.
  for (let attempt = 0; attempt < 120; attempt++) {
    if (containerFailure) {
      throw new Error("PayKesho container exited during startup", { cause: containerFailure });
    }
    try {
      const response = await port.fetch(`http://container${healthPath}`, {
        signal: AbortSignal.timeout(1000)
      });
      if (response.ok) {
        await response.body?.cancel();
        return;
      }
      await response.body?.cancel();
      lastError = new Error(`Health check returned ${response.status}`);
    } catch (error) {
      lastError = error;
    }
    await scheduler.wait(1000);
  }

  if (containerFailure) {
    throw new Error("PayKesho container exited during startup", { cause: containerFailure });
  }

  throw new Error(
    `Container did not become ready on port ${portNumber} within 120 seconds`,
    { cause: lastError }
  );
}

abstract class PaykeshoDirectContainer extends DurableObject<Env> {
  private starting?: Promise<void>;

  abstract readonly port: number;
  abstract readonly healthPath: string;
  abstract buildEnv(env: Env): Record<string, string>;

  async fetch(request: Request): Promise<Response> {
    const container = this.ctx.container;
    if (!container) {
      return Response.json(
        { ok: false, error: "container_unavailable" },
        { status: 503 }
      );
    }

    this.starting ??= waitForContainerPort(
      container,
      this.port,
      this.healthPath,
      this.buildEnv(this.env)
    ).finally(() => {
      this.starting = undefined;
    });

    await this.starting;

    return container.getTcpPort(this.port).fetch(
      rewriteRequestForContainer(request)
    );
  }
}

export class PaykeshoWebContainer extends PaykeshoDirectContainer {
  readonly port = 8080;
  readonly healthPath = "/admin/actuator/health";
  buildEnv(env: Env) {
    return backofficeEnv(env);
  }
}

export class PaykeshoPaymentContainer extends PaykeshoDirectContainer {
  readonly port = 8084;
  readonly healthPath = "/actuator/health/liveness";
  buildEnv(env: Env) {
    return paymentEnv(env);
  }
}

export class PaykeshoClientContainer extends PaykeshoDirectContainer {
  readonly port = 8080;
  readonly healthPath = "/actuator/health/liveness";
  buildEnv(env: Env) {
    return paykeshoEnv(env, "cloudflare");
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
        adminPasswordPresent: Boolean(env.PAYKESHO_ADMIN_PASSWORD)
      }, { headers: { "cache-control": "no-store" } });
    }

    const isPayment = url.pathname === "/webhooks/lenco"
      || url.pathname.startsWith("/internal/payment/");
    const isAdmin = url.pathname === "/admin" || url.pathname.startsWith("/admin/");
    const namespace = isPayment
      ? env.PAYKESHO_PAYMENT
      : (isAdmin ? env.PAYKESHO_WEB : env.PAYKESHO_CLIENT);
    const containerId = isPayment
      ? "payment"
      : (isAdmin ? "backoffice" : "client");

    try {
      if (isPayment && url.pathname.startsWith("/internal/payment/")) {
        const rewritten = new URL(request.url);
        rewritten.pathname =
          url.pathname.replace(/^\/internal\/payment/, "") || "/";
        return await namespace.getByName(containerId).fetch(
          new Request(rewritten.toString(), request)
        );
      }

      return await namespace.getByName(containerId).fetch(request);
    } catch (error) {
      console.error(`PayKesho ${containerId} container request failed`, error);
      return Response.json({
        ok: false,
        service: `paykesho-${containerId}`,
        error: "container_request_failed",
        message: error instanceof Error ? error.message : String(error)
      }, {
        status: 503,
        headers: { "cache-control": "no-store" }
      });
    }
  }
};
