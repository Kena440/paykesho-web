// Compact one-line-per-failure summary of a Playwright JSON report (CI annotations).
import fs from "node:fs";
const report = JSON.parse(fs.readFileSync(process.argv[2], "utf8"));
const strip = (s) => (s || "").replace(/\u001b\[[0-9;]*m/g, "");
const out = [];
let passed = 0, failed = 0;
function walk(suite, prefix) {
  for (const s of suite.suites || []) walk(s, prefix ? `${prefix} > ${s.title}` : s.title);
  for (const spec of suite.specs || []) {
    for (const t of spec.tests || []) {
      const r = t.results[t.results.length - 1];
      if (!r || r.status === "passed" || r.status === "skipped") { passed++; continue; }
      failed++;
      const msg = strip(r.error?.message).split("\n").filter((l) => l.trim()).slice(0, 4).join(" | ").slice(0, 330);
      out.push(`FAIL ${prefix} > ${spec.title} [${r.status}]: ${msg}`);
    }
  }
}
for (const s of report.suites) walk(s, "");
console.log(`passed ${passed}, failed ${failed}`);
console.log(out.join("\n"));
