// Local/CI stand-in for the Cloudflare Worker: one origin, path-routed to the
// portal processes using the same routing table and forwarded headers as
// src/index.ts. Run with: node --experimental-strip-types scripts/portal-router.mjs
//
// Upstreams come from env, e.g. CLIENT_URL=http://127.0.0.1:9001
import http from "node:http";
import { resolveRoute, forwardedHeaders } from "../src/routing.ts";

const port = Number(process.env.ROUTER_PORT || 8787);
const upstreams = {
  client: process.env.CLIENT_URL,
  backoffice: process.env.BACKOFFICE_URL,
  merchant: process.env.MERCHANT_URL,
  employer: process.env.EMPLOYER_URL,
  payment: process.env.PAYMENT_URL
};

const server = http.createServer((req, res) => {
  const publicUrl = new URL(req.url, `http://${req.headers.host || `localhost:${port}`}`);
  const route = resolveRoute(publicUrl.pathname);
  if (route.kind === "blocked") {
    res.writeHead(404, { "cache-control": "no-store" }).end("Not found");
    return;
  }
  const base = upstreams[route.portal];
  if (!base) {
    res.writeHead(503, { "content-type": "application/json" })
      .end(JSON.stringify({ ok: false, service: `paykesho-${route.portal}`, error: "container_unavailable" }));
    return;
  }
  const target = new URL(route.pathname + publicUrl.search, base);
  const incoming = new Headers();
  for (const [k, v] of Object.entries(req.headers)) {
    if (v !== undefined) incoming.set(k, Array.isArray(v) ? v.join(", ") : v);
  }
  const headers = Object.fromEntries(forwardedHeaders(publicUrl, incoming));
  headers.host = "container";

  const upstream = http.request(target, { method: req.method, headers }, (up) => {
    res.writeHead(up.statusCode || 502, up.headers);
    up.pipe(res);
  });
  upstream.on("error", (err) => {
    res.writeHead(503, { "content-type": "application/json" })
      .end(JSON.stringify({ ok: false, service: `paykesho-${route.portal}`, error: String(err) }));
  });
  req.pipe(upstream);
});

server.listen(port, "127.0.0.1", () => {
  console.log(`portal router listening on http://127.0.0.1:${port}`, upstreams);
});
