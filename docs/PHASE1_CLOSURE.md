# PayKesho Phase 1 — Foundations Closure Tracker

Updated: 2026-11-26
Repository: Kena440/paykesho-web
Baseline master: 5a8b6980b201c55b87381167876d29aa981fefb

## Delivery rule
Phase 1 is not complete until each item is implemented, integrated, secured, tested, and its evidence recorded. Passing CI does not equal production readiness. No production migration, deploy, or real-money action is authorised by this tracker.

## Status

| Workstream | Status | Evidence / next action |
|---|---|---|
| KYC registration + back-office review | Implemented, merged #36; follow-up compile fix in #39; review-row locking in #42 | #39 includes KYC document-access compile fix. #42 adds pessimistic locking for decision transitions to prevent concurrent approvals/rejections. Targeted tests included in CI. |
| Sensitive KYC document access | Implemented on #39 branch | NRC, selfie and payslip documents are reviewer-restricted; build/integration CI passed on #39 head. Review role authority mapping and test IDOR/access denial before release. |
| Ledger table foundation | Present in #35 migration | Tables exist as schema foundation; no chart of accounts or posting semantics approved. |
| Ledger journal integrity | Implemented on #39 branch | Deferred validation requires >=2 postings, debit=credit, single currency. SQL regression tests included; portal integration CI passed on #39 head. |
| Ledger immutability | Implemented on PR #39; account attributes hardened in PR #42 | Journal/posting UPDATE/DELETE are blocked; account code/name/type/owner/currency become immutable once used. SQL tests exercise account-currency mutation denial and active toggle. |
| Payment idempotency | Implemented on #39 branch | Caller-supplied key required; key bound to authenticated user + normalized merchant/amount; reservation failure fails closed. Build + portal integration CI passed. Need explicit concurrent-request and replay tests. |
| KYC private document storage | Partial / not verified | Existing file upload storage path still needs audit for private storage, URL exposure, access controls, retention and short-lived access. |
| Transaction PIN | In progress on PR #42 | PIN setup/change, BCrypt hashing, password re-authentication, 3-attempt lockout, payment confirmation enforcement, UI and unit tests. Recovery after lockout remains blocked pending verified recovery design. |
| Trusted devices | Schema only | Implement registration, verification, revocation, session/device binding and abuse controls. |
| Advance affordability/eligibility | Blocked by policy decision D-04 | No approved formula. Implement only after owner approval. |
| Interest/fees/repayment calculation | Blocked by policy decision D-17 and other financial decisions | Existing 15% value is not treated as approved policy. No customer-facing price or financial posting semantics to be hard-coded. |
| Ledger chart of accounts + posting rules | Blocked by owner approval | Design requires decisions on database of record, chart of accounts, posting semantics, fees, float/reserve, repayment, settlement and write-offs. |
| Provider-event/webhook deduplication | In progress on PR #42 | Signature verification already exists; PR #42 records verified, sanitized events using SHA-256 payload deduplication and processes settlement in the same transaction. Tests are being added. Provider-status polling and unknown-reference exception handling remain. |
| Reconciliation | Not implemented / not verified | Build provider-to-PayKesho transaction and ledger reconciliation with exception queue and audit trail. |
| CI and staging | CI exists; staging not verified | CI is disposable test environment. Confirm actual staging environment, deployment version, secrets/config and service health. |
| Release readiness | Not ready | Requires decision approvals, security review, tests, staging evidence, rollback plan and explicit release approval. |

## Verified GitHub evidence

- PR #36 KYC merged to master: merge commit 5a8b6980b201c55b87381167876d29aa981fefb.
- PR #37 KYC compile-fix CI failed on its isolated head.
- PR #39 includes the compile fix plus ledger integrity migration/tests and payment idempotency fail-closed changes.
- PR #39 head 97ec64ef5e6b24e20c70e5e831d1837663c531c6: Build Check passed; Portals Integration passed.\n- PR #42 is the PIN/account-hardening workstream; latest head is pending CI.
- PR #38 head b9179b4e2979a3354881ee4714e7a20f6a649eda: Build Check passed; Portals Integration passed. Its changes are included in PR #39.
- CI tests run against disposable PostgreSQL and Lenco test double; they do not prove production schema/deployment or real Lenco behavior.

## Decision gates

### D-04 — Advance limit and affordability formula
Required before eligibility/approval rules can be implemented. Recommendation: configurable conservative limit based on verified net salary, existing repayment commitments, employer-specific cap, and affordability guardrail; exact parameters to be approved by owner.

### D-17 — Interest/fees policy
Required before final repayment calculations and ledger posting semantics. Recommendation: keep policy configurable and inactive until written approval and legal review; do not treat the existing 15% as final.

Other required owner decisions: database of record/migration strategy, licensing route, credit rules, float/reserve, payout batching, repayment/payroll deduction rules, hosting region, chart of accounts/posting rules, and SMS provider.

## Next engineering order
1. Consolidate #39 as the Phase 1 technical baseline (it includes #37 and #38 changes); do not merge/deploy without release approval.
2. Finish KYC acceptance tests (registration, missing evidence, staff mismatch, pending sign-in, reviewer-only access, approve/reject/request-info, account activation).
3. Complete PIN and trusted-device services with unit/integration tests.
4. Implement provider-event/webhook security and reconciliation in test mode.
5. Build staging verification, deployment-health evidence and rollback checklist.
6. Resolve owner decisions before implementing credit policy, pricing, or financial posting rules.
7. Complete money-path test matrix and security review; obtain explicit Phase 1 gate approval.

## Financial test matrix (required)
Duplicate request; concurrent requests; forged webhook; duplicate webhook; provider timeout; failed payout with compensating reversal; insufficient float; balanced journal; reconciliation mismatch; uncertain outcome.
