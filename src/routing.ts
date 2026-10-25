// Path-based routing for the single PayKesho host.
//
// Every portal runs as its own Spring Boot container, mounted under its own
// servlet context path so that links, redirects and cookies stay inside the
// portal that issued them:
//
//   /admin/**                     back office          (context path /admin)
//   /merchant/**                  merchant portal      (context path /merchant)
//   /employer/**                  employer portal      (context path /employer)
//   /webhooks/lenco               payment service      (signed Lenco webhook)
//   /internal/payment/internal/v1/** payment service   (token-protected settlement API)
//   everything else               client portal        (context path /)
//
// Kept free of Cloudflare runtime imports so the same table drives the Worker
// and the local/CI integration proxy (scripts/portal-router.mjs).

export type PortalId = "client" | "backoffice" | "merchant" | "employer" | "payment";

export type Route =
  | { kind: "blocked" }
  | { kind: "portal"; portal: PortalId; pathname: string };

function mountedAt(pathname: string, prefix: string): boolean {
  return pathname === prefix || pathname.startsWith(prefix + "/");
}

export function resolveRoute(pathname: string): Route {
  // Only the token-protected settlement API and the signed Lenco webhook may
  // reach the payment container from outside. The legacy /rest/api/** payment
  // endpoints have no authentication and must never be publicly routable.
  if (pathname.startsWith("/internal/payment/")) {
    if (!pathname.startsWith("/internal/payment/internal/v1/")) {
      return { kind: "blocked" };
    }
    return {
      kind: "portal",
      portal: "payment",
      pathname: pathname.replace(/^\/internal\/payment/, "") || "/"
    };
  }
  if (pathname === "/webhooks/lenco") {
    return { kind: "portal", portal: "payment", pathname };
  }
  if (mountedAt(pathname, "/admin")) {
    return { kind: "portal", portal: "backoffice", pathname };
  }
  if (mountedAt(pathname, "/merchant")) {
    return { kind: "portal", portal: "merchant", pathname };
  }
  if (mountedAt(pathname, "/employer")) {
    return { kind: "portal", portal: "employer", pathname };
  }
  return { kind: "portal", portal: "client", pathname };
}

// Containers are reached at http://container/..., so Spring would otherwise
// build redirects, absolute links and cookie flags for host "container" over
// plain HTTP. These headers carry the public origin through; the portals honour
// them via server.forward-headers-strategy=framework.
export function forwardedHeaders(publicUrl: URL, original: Headers): Headers {
  const headers = new Headers(original);
  headers.delete("host");
  headers.set("x-forwarded-host", publicUrl.host);
  headers.set("x-forwarded-proto", publicUrl.protocol.replace(":", ""));
  headers.set("x-forwarded-port", publicUrl.port || (publicUrl.protocol === "https:" ? "443" : "80"));
  return headers;
}
