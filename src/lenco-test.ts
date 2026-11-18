// Lenco test double for the testing stage. Never moves money.
//
// When the Worker variable LENCO_TEST_MODE is "true":
//   * the payment service is pointed at <public origin>/internal/lenco-test
//     instead of the live Lenco API, with placeholder credentials, so no real
//     Lenco token is used and live Lenco cannot be reached, and
//   * this module answers the two Lenco calls the payment service makes:
//       POST /transfers/mobile-money | /transfers/bank-account  -> successful
//       GET  /transfers/status/{reference}                      -> successful
//
// It keeps no state: every transfer is reported successful and every status
// look-up reports successful, with lencoReference "TEST-<reference>". That is
// enough for testers to walk the whole customer journey (merchant gets
// "paid", balances and the ledger move) while no money leaves any account.
//
// When LENCO_TEST_MODE is anything else, the path returns 404 and the payment
// service gets the real LENCO_API_BASE_URL / LENCO_API_TOKEN / LENCO_ACCOUNT_ID.
//
// Kept free of Cloudflare runtime imports so it can be unit-tested with Node.

export const LENCO_TEST_PREFIX = "/internal/lenco-test";
export const LENCO_TEST_CREDENTIAL = "paykesho-test-mode-no-money";

export function lencoTestModeOn(flag: string | undefined): boolean {
  return (flag || "").trim().toLowerCase() === "true";
}

export interface LencoSettings {
  LENCO_API_BASE_URL: string;
  LENCO_API_TOKEN: string;
  LENCO_ACCOUNT_ID: string;
}

export function lencoSettings(opts: {
  testMode: string | undefined;
  publicOrigin: string;
  baseUrl?: string;
  token?: string;
  accountId?: string;
}): LencoSettings {
  if (lencoTestModeOn(opts.testMode)) {
    return {
      LENCO_API_BASE_URL: opts.publicOrigin.replace(/\/$/, "") + LENCO_TEST_PREFIX,
      LENCO_API_TOKEN: LENCO_TEST_CREDENTIAL,
      LENCO_ACCOUNT_ID: LENCO_TEST_CREDENTIAL
    };
  }
  return {
    LENCO_API_BASE_URL: opts.baseUrl || "https://api.lenco.co/access/v2",
    LENCO_API_TOKEN: opts.token || "",
    LENCO_ACCOUNT_ID: opts.accountId || ""
  };
}

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json", "cache-control": "no-store" }
  });
}

export async function handleLencoTest(request: Request, testMode: string | undefined): Promise<Response> {
  if (!lencoTestModeOn(testMode)) {
    return new Response("Not found", { status: 404, headers: { "cache-control": "no-store" } });
  }
  const path = new URL(request.url).pathname.slice(LENCO_TEST_PREFIX.length);
  if (request.headers.get("authorization") !== `Bearer ${LENCO_TEST_CREDENTIAL}`) {
    return json(401, { status: false, message: "Missing or wrong bearer token" });
  }

  if (request.method === "POST" && /^\/transfers\/(mobile-money|bank-account)$/.test(path)) {
    let body: { reference?: unknown; amount?: unknown };
    try {
      body = (await request.json()) as typeof body;
    } catch {
      return json(400, { status: false, message: "Invalid JSON" });
    }
    const reference = typeof body.reference === "string" ? body.reference.trim() : "";
    const amount = Number(body.amount);
    if (!reference || !(amount > 0)) {
      return json(400, { status: false, message: "reference and amount are required" });
    }
    return json(200, {
      status: true,
      message: "Test mode: transfer reported successful, no money moved",
      data: { reference, lencoReference: `TEST-${reference}`, status: "successful", amount: String(body.amount) }
    });
  }

  const status = path.match(/^\/transfers\/status\/(.+)$/);
  if (request.method === "GET" && status) {
    const reference = decodeURIComponent(status[1]);
    return json(200, {
      status: true,
      message: "Test mode",
      data: { reference, lencoReference: `TEST-${reference}`, status: "successful" }
    });
  }

  return json(404, { status: false, message: `Test mode has no handler for ${request.method} ${path}` });
}
