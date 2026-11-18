// Unit tests for the Lenco test double (src/lenco-test.ts). Run: node --experimental-strip-types --test scripts/lenco-test.test.mjs
import test from "node:test";
import assert from "node:assert/strict";
import { handleLencoTest, lencoSettings, LENCO_TEST_CREDENTIAL } from "../src/lenco-test.ts";

const base = "https://paykesho.example/internal/lenco-test";
const auth = { authorization: `Bearer ${LENCO_TEST_CREDENTIAL}`, "content-type": "application/json" };

test("off by default: 404 and real Lenco settings", async () => {
  const r = await handleLencoTest(new Request(base + "/transfers/status/x", { headers: auth }), undefined);
  assert.equal(r.status, 404);
  const s = lencoSettings({ testMode: "false", publicOrigin: "https://p.example", token: "real", accountId: "acc" });
  assert.equal(s.LENCO_API_BASE_URL, "https://api.lenco.co/access/v2");
  assert.equal(s.LENCO_API_TOKEN, "real");
});

test("test mode overrides real credentials and base URL", () => {
  const s = lencoSettings({ testMode: "true", publicOrigin: "https://p.example", baseUrl: "https://api.lenco.co/access/v2", token: "real", accountId: "acc" });
  assert.equal(s.LENCO_API_BASE_URL, "https://p.example/internal/lenco-test");
  assert.equal(s.LENCO_API_TOKEN, LENCO_TEST_CREDENTIAL);
  assert.notEqual(s.LENCO_ACCOUNT_ID, "acc");
});

test("transfer reported successful", async () => {
  for (const kind of ["mobile-money", "bank-account"]) {
    const r = await handleLencoTest(new Request(`${base}/transfers/${kind}`, { method: "POST", headers: auth, body: JSON.stringify({ reference: "PK-1", amount: "120.50" }) }), "true");
    assert.equal(r.status, 200);
    const b = await r.json();
    assert.equal(b.data.status, "successful");
    assert.equal(b.data.lencoReference, "TEST-PK-1");
  }
});

test("status look-up successful", async () => {
  const r = await handleLencoTest(new Request(base + "/transfers/status/PK-1", { headers: auth }), "true");
  assert.equal((await r.json()).data.status, "successful");
});

test("rejects missing token and bad input", async () => {
  assert.equal((await handleLencoTest(new Request(base + "/transfers/status/x"), "true")).status, 401);
  const bad = await handleLencoTest(new Request(base + "/transfers/mobile-money", { method: "POST", headers: auth, body: JSON.stringify({ reference: "", amount: 0 }) }), "true");
  assert.equal(bad.status, 400);
});
