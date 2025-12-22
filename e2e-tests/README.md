# Paykesho E2E Tests

End-to-end tests for the Paykesho web application using Playwright.

## Setup

1. Install dependencies:
```bash
npm install
```

2. Install browsers:
```bash
npm run install:browsers
```

## Running Tests

- Run all tests: `npm test`
- Run with UI: `npm run test:ui`
- Run in headed mode: `npm run test:headed`
- Debug tests: `npm run test:debug`
- View report: `npm run test:report`

## Test Structure

- `tests/client-registration.spec.ts` - Client registration flow tests
- `tests/voucher-approval.spec.ts` - Voucher approval flow tests
- More test files to be added...

## Configuration

Update `playwright.config.ts` to configure:
- Base URL
- Test environment
- Browser configurations
- Timeouts and retries
