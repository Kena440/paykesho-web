# PayKesho Lenco Settlement Setup

PayKesho uses Lenco as the external merchant payout rail for Zambia.

## Required Cloudflare secrets

Set these as Cloudflare Worker/Container secrets. Never commit their values.

- `LENCO_API_TOKEN` — Lenco API token.
- `LENCO_ACCOUNT_ID` — the 36-character Lenco account UUID to debit.
- `PAYKESHO_INTERNAL_TOKEN` — a strong random secret shared only by the customer and payment containers.

## Required environment variable

- `LENCO_API_BASE_URL=https://api.lenco.co/access/v2`
- `PAYMENT_SERVICE_URL=<public PayKesho Worker URL>/internal/payment`

The payment-service URL must be reachable from the customer container. Do not use `localhost` in production.

## Lenco webhook

Configure the Lenco dashboard webhook URL as:

`https://<paykesho-domain>/webhooks/lenco`

The endpoint must be publicly reachable. PayKesho verifies `X-Lenco-Signature` using the SHA-256 hash of the API token as the HMAC-SHA512 key.

Handled events:

- `transfer.successful`
- `transfer.failed`

PayKesho also re-queries pending transfers periodically using Lenco's transfer-status endpoint.

## Merchant payout profiles

For each enabled merchant configure one of:

### Mobile money

- Payout method: `mobile-money`
- Operator: `airtel`, `mtn`, or `zamtel`
- Payout phone, unless a Lenco transfer-recipient UUID is configured.

### Bank account

- Payout method: `bank-account`
- Bank ID
- Account number

A Lenco transfer-recipient UUID can be used instead of the underlying payout details.

## Payment lifecycle

1. PayKesho validates the merchant payout profile.
2. PayKesho atomically consumes customer e-value and creates a PENDING transaction.
3. PayKesho initiates the Lenco payout using the PayKesho transaction reference.
4. SUCCESSFUL marks the transaction settled.
5. FAILED releases the consumed e-value.
6. PENDING remains pending until webhook or status re-query resolves it.

Do not mark a transaction settled based only on receiving an HTTP 200 from Lenco; Lenco's transfer status is carried in the response data and may be pending or failed.
