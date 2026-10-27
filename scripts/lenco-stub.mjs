// CI-only stand-in for the Lenco transfers API (never used in production).
// Accepts mobile-money and bank-account transfers and reports them successful,
// and answers status look-ups for references it has seen. Lets the portal suite
// exercise merchant settlement end to end without moving money.
//
// Run with: node scripts/lenco-stub.mjs   (LENCO_STUB_PORT, default 9099)
import http from "node:http";

const port = Number(process.env.LENCO_STUB_PORT || 9099);
const transfers = new Map();

function send(res, status, body) {
  res.writeHead(status, { "content-type": "application/json" }).end(JSON.stringify(body));
}

const server = http.createServer((req, res) => {
  const url = new URL(req.url, `http://127.0.0.1:${port}`);
  if (!/^Bearer \S+/.test(req.headers.authorization || "")) {
    return send(res, 401, { status: false, message: "Missing bearer token" });
  }
  let raw = "";
  req.on("data", (c) => (raw += c));
  req.on("end", () => {
    if (req.method === "POST" && /\/transfers\/(mobile-money|bank-account)$/.test(url.pathname)) {
      let body = {};
      try { body = JSON.parse(raw || "{}"); } catch { return send(res, 400, { status: false, message: "Invalid JSON" }); }
      if (!body.reference || !(Number(body.amount) > 0)) {
        return send(res, 400, { status: false, message: "reference and amount are required" });
      }
      const existing = transfers.get(body.reference);
      if (existing) return send(res, 200, { status: true, message: "Duplicate reference", data: existing });
      const data = { reference: body.reference, lencoReference: `STUB-${transfers.size + 1}`, status: "successful", amount: String(body.amount) };
      transfers.set(body.reference, data);
      console.log(`transfer ${url.pathname} ${body.reference} ${body.amount}`);
      return send(res, 200, { status: true, message: "Transfer successful", data });
    }
    const status = url.pathname.match(/\/transfers\/status\/(.+)$/);
    if (req.method === "GET" && status) {
      const data = transfers.get(decodeURIComponent(status[1]));
      return data ? send(res, 200, { status: true, message: "", data }) : send(res, 404, { status: false, message: "Not found" });
    }
    send(res, 404, { status: false, message: `No stub for ${req.method} ${url.pathname}` });
  });
});

server.listen(port, "127.0.0.1", () => console.log(`Lenco stub on http://127.0.0.1:${port}`));
