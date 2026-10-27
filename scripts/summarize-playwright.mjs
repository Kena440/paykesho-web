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
      // Keep list-style failures (e.g. the page crawl) whole; others to a few lines.
      const lines = strip(r.error?.message).split("\n").filter((l) => l.trim());
      const msg = lines.slice(0, lines.length > 6 && /\]$|\[/.test(lines.join("")) ? 80 : 4).join(" | ").slice(0, 6000);
      out.push(`FAIL ${prefix} > ${spec.title} [${r.status}]: ${msg}`);
    }
  }
}
for (const s of report.suites) walk(s, "");
// Crawl coverage lines (what was visited / skipped) from the tests' own output.
const info = [];
(function collect(suite) {
  for (const s of suite.suites || []) collect(s);
  for (const spec of suite.specs || []) for (const t of spec.tests || []) for (const r of t.results || [])
    for (const o of r.stdout || []) { const text = strip(o.text || ""); if (text.startsWith("[crawl")) info.push(text.trim().slice(0, 1500)); }
})({ suites: report.suites });
console.log(`passed ${passed}, failed ${failed}`);
console.log(out.join("\n"));
if (info.length) console.log(info.join("\n"));
