# E2E Test Suites

This directory contains end-to-end tests for all critical user flows in the Paykesho application.

## Test Files

### Authentication & Security
- **login-authentication-flow.spec.ts** - Login flows for all portals, logout, account locking
- **password-reset-flow.spec.ts** - Password reset request, token validation, password update

### Client Portal
- **client-registration.spec.ts** - Client registration with validation
- **voucher-request-flow.spec.ts** - Voucher request with payslip upload, validation
- **voucher-approval.spec.ts** - Voucher approval with idempotency checks
- **transaction-payment-flow.spec.ts** - Transaction initiation, payment processing, balance updates
- **complete-voucher-lifecycle.spec.ts** - End-to-end voucher lifecycle (request → approve → use)

### Backoffice Admin Portal
- **admin-merchant-management.spec.ts** - Merchant CRUD operations, enable/disable
- **admin-user-management.spec.ts** - User management, role assignment, enable/disable

### Organization Portal
- **organization-client-management.spec.ts** - Client management by organization
- **organization-repayment-flow.spec.ts** - Repayment schedule creation, payroll deductions, payment processing

### Payment Service
- **payment-api-flow.spec.ts** - Payment API endpoints, idempotency, webhook callbacks

### File Management
- **file-upload-flow.spec.ts** - File uploads (payslips, logos), validation, security checks

## Test Coverage

### Critical Business Flows Covered:
1. ✅ Client Registration
2. ✅ Voucher Request & Approval
3. ✅ Transaction Processing
4. ✅ Payment Processing (Bank, MoMo, Card)
5. ✅ Repayment Processing
6. ✅ Password Reset
7. ✅ Merchant Management
8. ✅ User Management
9. ✅ File Uploads
10. ✅ Authentication & Authorization

### Areas for Additional E2E Tests:
- Settlement processing flows
- Notification delivery verification
- OAuth login flows
- Multi-step payment flows
- Report generation
- Search and filtering
- Pagination
- Export functionality

## Running Specific Test Suites

```bash
# Run all tests
npm test

# Run specific test file
npx playwright test tests/voucher-request-flow.spec.ts

# Run tests in headed mode
npx playwright test --headed

# Run tests with UI mode
npx playwright test --ui
```

## Test Data Requirements

Tests assume the following test data exists:
- Admin user: `admin@paykesho.com` / `admin123`
- Client user: `client@example.com` / `password123`
- Organization admin: `orgadmin@example.com` / `password123`
- Test merchant: `MERCHANT001`

## Notes

- Tests use realistic selectors but may need adjustment based on actual HTML structure
- Some tests require test data setup before running
- Payment gateway URLs may need mocking for CI/CD environments
- File uploads use in-memory buffers for testing
