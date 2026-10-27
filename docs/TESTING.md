# PayKesho — test-stage guide

One site, four portals (all on the same address):

| Portal | Path | Who |
|---|---|---|
| Client | `/` (Register / Sign in) | Employees (Thomas in the customer journey) |
| Back office | `/admin` | PayKesho staff |
| Merchant | `/merchant` | Shop owners |
| Employer | `/employer` | Employer HR |

Use test data only. No real customer data, and no real money (see "Payments" below).

## Ready-made test accounts (production database)

- **Back office:** the administrator account (`admin@paykesho.com`, password held by Amukena).
- **Employer portal:** "PayKesho Demo Employer (DEMO)" — sign in with the same email and password as the back office.
- **Merchant portal:** "PayKesho Demo Shop", merchant code `M20261DEMO1` — same email and password.
- **Client:** register at `/` → Register, choosing employer "PayKesho Demo Employer (DEMO)" and these staff-list details: name **Demo Employee**, NRC **999999/99/1**, TPIN **9000000001**, man number **DEMO-001**. Details that don't match the staff list are refused (by design).

Testers can also create their own employers and merchants in the back office (below).

## Customer journey to test

1. **Client registers** (as above) and signs in.
2. **Client requests an advance** — Vouchers → Request: amount, reason, repayment period, and a payslip (PDF, PNG or JPEG, up to 1 MB). The cost (15% interest) and repayment dates are shown before submitting.
3. **Back office reviews and approves** — `/admin` → Vouchers: open the payslip, then Approve. (On a narrow screen the Actions column folds into the row's "+" control.)
4. **Client pays a merchant** — Pay: enter the merchant code (or scan the merchant's QR code), check the merchant's number and name, enter the amount, Pay Now. The result shows the reference and remaining balance; the dashboard balance goes down.
5. **Merchant sees the payment** — `/merchant` → Transactions; the dashboard shows the merchant's QR code.
6. **Employer sees the advance but not the purchases** — `/employer` → Vouchers / Clients.
7. **Back office sees the payment and its settlement status** — `/admin` → Transactions.

## Onboarding a new employer or merchant (back office)

- **Employer:** Organizations → New (logo required) → open it → Employees tab → Add Employee (the staff list clients register against) → Assign Admin.
- **Merchant:** Merchants → New (logo required; payout method and number) → open it → add the merchant admin.
- After adding an admin, the back office shows the new admin's **password once**, with the sign-in link. Pass it on securely; email and SMS are not being sent yet. "Reset Password" works the same way.
- Every user can change their own password under the profile menu → Profile (current password required).

## Payments (Lenco) — read before testing payments

Production is configured with Lenco's **live** API address. A payment to a merchant **with a payout profile** would try to send real money through Lenco if a live key is set. Until Lenco's sandbox address and a sandbox key are configured (Cloudflare: `LENCO_API_BASE_URL`, `LENCO_API_TOKEN`, `LENCO_ACCOUNT_ID`), only pay the Demo Shop, which has no payout profile: the payment stops with "Merchant payout method is not configured" and the e-value is not spent.

## Not in this test stage (known, not defects)

- Email and SMS notifications are queued but not sent (the notification service is not deployed and no SMS provider is chosen). This includes "forgot password" emails.
- New client accounts are active immediately; the spec's back-office KYC approval of registrations, NRC upload and first/middle/last-name fields are not built (decisions needed).
- No advance limit check (D-04); interest fixed at 15% (D-17); no transaction PIN.
- Payroll deductions in the employer portal record repayments without matching them to money received — do not rely on them for testing repayments.
- The Android app is not part of this stage (web only).

## Reporting a problem

Note the portal, the page address, what you did, what you expected and what happened, with a screenshot.

## Automated checks

Every pull request runs the "Portals integration" workflow: all portals behind one address, the customer journey above, back-office onboarding, and a visit to every page in every portal looking for errors (`e2e-tests/portals`).
