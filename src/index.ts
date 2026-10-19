import { Container, ContainerProxy, getContainer } from "@cloudflare/containers";
import { env } from "cloudflare:workers";

export interface Env {
  PAYKESHO_CLIENT: DurableObjectNamespace<ClientPortal>;
  PAYKESHO_MERCHANT: DurableObjectNamespace<MerchantPortal>;
  PAYKESHO_ORGANIZATION: DurableObjectNamespace<OrganizationPortal>;
  PAYKESHO_BACKOFFICE: DurableObjectNamespace<Backoffice>;
  PAYKESHO_PAYMENT: DurableObjectNamespace<PaymentService>;
  PAYKESHO_NOTIFICATION: DurableObjectNamespace<NotificationService>;
  PAYKESHO_FILE: DurableObjectNamespace<FileService>;
  SUPABASE_DB_URL: string;
  GOOGLE_CLIENT_ID?: string;
  GOOGLE_CLIENT_SECRET?: string;
  FACEBOOK_CLIENT_ID?: string;
  FACEBOOK_CLIENT_SECRET?: string;
  ZYNLEPAY_MERCHANT_ID?: string;
  ZYNLEPAY_API_ID?: string;
  ZYNLEPAY_API_KEY?: string;
  ZYNLEPAY_URL?: string;
  SMTP_HOST?: string;
  SMTP_PORT?: string;
  SMTP_USERNAME?: string;
  SMTP_PASSWORD?: string;
  NOTIFICATION_PASSWORD?: string;
  NOTIFICATION_USERNAME?: string;
  RATE_LIMITING_ENABLED?: string;
  FILE_PUBLIC_BASE_URL?: string;
}

function proxy(binding: keyof Env, name: string) {
  return async (request: Request, env: Env) =>
    getContainer(env[binding] as DurableObjectNamespace<any>, name).fetch(request);
}

export class ClientPortal extends Container {
  defaultPort = 8080;
  sleepAfter = "30m";
  entrypoint = ["/app/run-service.sh", "client-portal"];
  envVars = { SUPABASE_DB_URL: env.SUPABASE_DB_URL, GOOGLE_CLIENT_ID: env.GOOGLE_CLIENT_ID ?? "", GOOGLE_CLIENT_SECRET: env.GOOGLE_CLIENT_SECRET ?? "", FACEBOOK_CLIENT_ID: env.FACEBOOK_CLIENT_ID ?? "", FACEBOOK_CLIENT_SECRET: env.FACEBOOK_CLIENT_SECRET ?? "", RATE_LIMITING_ENABLED: env.RATE_LIMITING_ENABLED ?? "true", NOTIFICATION_SERVICE_URL: "http://notification.internal", FILE_SERVICE_URL: "http://file.internal" };
  static outboundByHost = {
    "notification.internal": proxy("PAYKESHO_NOTIFICATION", "notification"),
    "file.internal": proxy("PAYKESHO_FILE", "file"),
  };
}

export class MerchantPortal extends Container {
  defaultPort = 8080;
  sleepAfter = "30m";
  entrypoint = ["/app/run-service.sh", "merchant-portal"];
  envVars = { SUPABASE_DB_URL: env.SUPABASE_DB_URL, GOOGLE_CLIENT_ID: env.GOOGLE_CLIENT_ID ?? "", GOOGLE_CLIENT_SECRET: env.GOOGLE_CLIENT_SECRET ?? "", FACEBOOK_CLIENT_ID: env.FACEBOOK_CLIENT_ID ?? "", FACEBOOK_CLIENT_SECRET: env.FACEBOOK_CLIENT_SECRET ?? "", RATE_LIMITING_ENABLED: env.RATE_LIMITING_ENABLED ?? "true", NOTIFICATION_SERVICE_URL: "http://notification.internal", FILE_SERVICE_URL: "http://file.internal" };
  static outboundByHost = {
    "notification.internal": proxy("PAYKESHO_NOTIFICATION", "notification"),
    "file.internal": proxy("PAYKESHO_FILE", "file"),
  };
}

export class OrganizationPortal extends Container {
  defaultPort = 8080;
  sleepAfter = "30m";
  entrypoint = ["/app/run-service.sh", "organization-portal"];
  envVars = { SUPABASE_DB_URL: env.SUPABASE_DB_URL, RATE_LIMITING_ENABLED: env.RATE_LIMITING_ENABLED ?? "true", NOTIFICATION_SERVICE_URL: "http://notification.internal" };
  static outboundByHost = {
    "notification.internal": proxy("PAYKESHO_NOTIFICATION", "notification"),
  };
}

export class Backoffice extends Container {
  defaultPort = 8080;
  sleepAfter = "30m";
  entrypoint = ["/app/run-service.sh", "backoffice"];
  envVars = { SUPABASE_DB_URL: env.SUPABASE_DB_URL, RATE_LIMITING_ENABLED: env.RATE_LIMITING_ENABLED ?? "true", NOTIFICATION_SERVICE_URL: "http://notification.internal", FILE_SERVICE_URL: "http://file.internal" };
  static outboundByHost = {
    "notification.internal": proxy("PAYKESHO_NOTIFICATION", "notification"),
    "file.internal": proxy("PAYKESHO_FILE", "file"),
  };
}

export class PaymentService extends Container {
  defaultPort = 8080;
  sleepAfter = "30m";
  entrypoint = ["/app/run-service.sh", "payment-service"];
  envVars = { SUPABASE_DB_URL: env.SUPABASE_DB_URL, ZYNLEPAY_MERCHANT_ID: env.ZYNLEPAY_MERCHANT_ID ?? "", ZYNLEPAY_API_ID: env.ZYNLEPAY_API_ID ?? "", ZYNLEPAY_API_KEY: env.ZYNLEPAY_API_KEY ?? "", ZYNLEPAY_URL: env.ZYNLEPAY_URL ?? "https://sandbox.zynlepay.com/zynlepay/jsonapi", RATE_LIMITING_ENABLED: env.RATE_LIMITING_ENABLED ?? "true", NOTIFICATION_SERVICE_URL: "http://notification.internal" };
  static outboundByHost = {
    "notification.internal": proxy("PAYKESHO_NOTIFICATION", "notification"),
  };
}

export class NotificationService extends Container {
  defaultPort = 8080;
  sleepAfter = "30m";
  entrypoint = ["/app/run-service.sh", "notification-service"];
  envVars = { SUPABASE_DB_URL: env.SUPABASE_DB_URL, SMTP_HOST: env.SMTP_HOST ?? "smtp.gmail.com", SMTP_PORT: env.SMTP_PORT ?? "587", SMTP_USERNAME: env.SMTP_USERNAME ?? "", SMTP_PASSWORD: env.SMTP_PASSWORD ?? "", NOTIFICATION_PASSWORD: env.NOTIFICATION_PASSWORD ?? "", NOTIFICATION_USERNAME: env.NOTIFICATION_USERNAME ?? "Paykesho", RATE_LIMITING_ENABLED: env.RATE_LIMITING_ENABLED ?? "true" };
}

export class FileService extends Container {
  defaultPort = 8080;
  sleepAfter = "30m";
  entrypoint = ["/app/run-service.sh", "file-service"];
  envVars = { SUPABASE_DB_URL: env.SUPABASE_DB_URL, RATE_LIMITING_ENABLED: env.RATE_LIMITING_ENABLED ?? "true", FILE_STORAGE_UPLOAD_DIR: "/app/uploads", FILE_PUBLIC_BASE_URL: env.FILE_PUBLIC_BASE_URL ?? "" };
}

function route(prefix: string, binding: keyof Env, name: string, request: Request, env: Env) {
  const url = new URL(request.url);
  url.pathname = url.pathname.slice(prefix.length) || "/";
  return getContainer(env[binding] as DurableObjectNamespace<any>, name).fetch(new Request(url, request));
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const path = new URL(request.url).pathname;
    if (path === "/health") return Response.json({ status: "ok", service: "paykesho-cloudflare" });
    if (path.startsWith("/client/")) return route("/client", "PAYKESHO_CLIENT", "client", request, env);
    if (path.startsWith("/merchant/")) return route("/merchant", "PAYKESHO_MERCHANT", "merchant", request, env);
    if (path.startsWith("/organization/")) return route("/organization", "PAYKESHO_ORGANIZATION", "organization", request, env);
    if (path.startsWith("/admin/")) return route("/admin", "PAYKESHO_BACKOFFICE", "backoffice", request, env);
    if (path.startsWith("/payments/")) return route("/payments", "PAYKESHO_PAYMENT", "payment", request, env);
    if (path.startsWith("/notifications/")) return route("/notifications", "PAYKESHO_NOTIFICATION", "notification", request, env);
    if (path.startsWith("/files/")) return route("/files", "PAYKESHO_FILE", "file", request, env);
    return new Response("PayKesho Cloudflare gateway", { status: 404 });
  },
};

export { ContainerProxy };
