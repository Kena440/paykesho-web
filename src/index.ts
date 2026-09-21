import { Container, ContainerProxy, getContainer } from "@cloudflare/containers";

export interface Env {
  PAYKESHO_CLIENT: DurableObjectNamespace<ClientPortal>;
  PAYKESHO_MERCHANT: DurableObjectNamespace<MerchantPortal>;
  PAYKESHO_ORGANIZATION: DurableObjectNamespace<OrganizationPortal>;
  PAYKESHO_BACKOFFICE: DurableObjectNamespace<Backoffice>;
  PAYKESHO_PAYMENT: DurableObjectNamespace<PaymentService>;
  PAYKESHO_NOTIFICATION: DurableObjectNamespace<NotificationService>;
  PAYKESHO_FILE: DurableObjectNamespace<FileService>;
}

function proxy(binding: keyof Env, name: string) {
  return async (request: Request, env: Env) =>
    getContainer(env[binding] as DurableObjectNamespace<any>, name).fetch(request);
}

export class ClientPortal extends Container {
  defaultPort = 8080;
  sleepAfter = "30m";
  entrypoint = ["/app/run-service.sh", "client-portal"];
  static outboundByHost = {
    "notification.internal": proxy("PAYKESHO_NOTIFICATION", "notification"),
    "file.internal": proxy("PAYKESHO_FILE", "file"),
  };
}

export class MerchantPortal extends Container {
  defaultPort = 8080;
  sleepAfter = "30m";
  entrypoint = ["/app/run-service.sh", "merchant-portal"];
  static outboundByHost = {
    "notification.internal": proxy("PAYKESHO_NOTIFICATION", "notification"),
    "file.internal": proxy("PAYKESHO_FILE", "file"),
  };
}

export class OrganizationPortal extends Container {
  defaultPort = 8080;
  sleepAfter = "30m";
  entrypoint = ["/app/run-service.sh", "organization-portal"];
  static outboundByHost = {
    "notification.internal": proxy("PAYKESHO_NOTIFICATION", "notification"),
  };
}

export class Backoffice extends Container {
  defaultPort = 8080;
  sleepAfter = "30m";
  entrypoint = ["/app/run-service.sh", "backoffice"];
  static outboundByHost = {
    "notification.internal": proxy("PAYKESHO_NOTIFICATION", "notification"),
    "file.internal": proxy("PAYKESHO_FILE", "file"),
  };
}

export class PaymentService extends Container {
  defaultPort = 8080;
  sleepAfter = "30m";
  entrypoint = ["/app/run-service.sh", "payment-service"];
  static outboundByHost = {
    "notification.internal": proxy("PAYKESHO_NOTIFICATION", "notification"),
  };
}

export class NotificationService extends Container {
  defaultPort = 8080;
  sleepAfter = "30m";
  entrypoint = ["/app/run-service.sh", "notification-service"];
}

export class FileService extends Container {
  defaultPort = 8080;
  sleepAfter = "30m";
  entrypoint = ["/app/run-service.sh", "file-service"];
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
