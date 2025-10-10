# Idempotency Implementation Guide

## Overview

This guide documents the comprehensive idempotency implementation across all Paykesho modules to prevent duplicate operations and ensure data consistency.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Database Schema](#database-schema)
3. [Implementation by Module](#implementation-by-module)
4. [Critical Implementations](#critical-implementations)
5. [Testing Idempotency](#testing-idempotency)
6. [Best Practices](#best-practices)
7. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

### Core Components

#### 1. **Idempotency Entity** (`commons/entity/IdempotencyRecord.java`)
- Stores idempotency keys with request/response data
- Tracks processing status (PROCESSING, COMPLETED, FAILED)
- Automatic expiration after 24 hours
- Request hash validation to prevent replay attacks

#### 2. **Idempotency Service** (per module)
- Located in each module: `{module}/idempotency/service/IdempotencyService.java`
- Provides:
  - `checkIdempotency(String key)` - Check if operation was already processed
  - `createIdempotencyRecord()` - Mark operation as started
  - `markAsCompleted()` - Mark operation as successfully completed
  - `markAsFailed()` - Mark operation as failed
  - `cleanupExpiredRecords()` - Remove old records

#### 3. **Idempotency Repository** (per module)
- Located in each module: `{module}/idempotency/repository/IdempotencyRecordRepository.java`
- Provides database access for idempotency records

### Design Principles

1. **Decoupled**: Each module has its own idempotency service (no cross-module dependencies)
2. **Database-backed**: No in-memory storage (works in distributed environments)
3. **Automatic cleanup**: Expired records cleaned up automatically
4. **Request validation**: Hash-based validation prevents replay attacks with different payloads

---

## Database Schema

### Idempotency Records Table

```sql
CREATE TABLE idempotency_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    endpoint VARCHAR(500) NOT NULL,
    http_method VARCHAR(10),
    request_hash VARCHAR(64),
    request_body TEXT,
    response_status INT,
    response_body TEXT,
    response_headers TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    user_identifier VARCHAR(255),
    metadata TEXT,
    error_message TEXT,
    attempt_count INT DEFAULT 1,
    last_attempt_at TIMESTAMP,
    
    INDEX idx_idempotency_key (idempotency_key),
    INDEX idx_expires_at (expires_at),
    INDEX idx_status (status)
);
```

### Business Constraint Enhancements

```sql
-- Prevent duplicate settlements
ALTER TABLE settlements 
ADD UNIQUE INDEX uk_settlement_idempotency (merchant_id, DATE(settlement_date), settlement_status);

-- Prevent duplicate payment callbacks
ALTER TABLE payments
ADD UNIQUE INDEX uk_payment_reference (payment_reference, payment_method);

-- Prevent duplicate repayment schedules
ALTER TABLE repayment_schedules
ADD UNIQUE INDEX uk_voucher_installment (voucher_id, installment_number);

-- Optimistic locking for balances
ALTER TABLE client_balances
ADD COLUMN version BIGINT DEFAULT 0;
```

---

## Implementation by Module

### 1. Payment Service

**Location**: `webparent/payment-service/`

#### Implementations:

##### A. Payment Processing
**File**: `payments/services/PaymentService.java`

**Before** (In-memory, single-instance only):
```java
private final ConcurrentHashMap<String, String> processedPayments = new ConcurrentHashMap<>();

public boolean isPaymentAlreadyProcessed(String idempotencyKey) {
    return processedPayments.containsKey(idempotencyKey);
}
```

**After** (Database-backed, distributed-safe):
```java
private final IdempotencyService idempotencyService;

public boolean isPaymentAlreadyProcessed(String idempotencyKey) {
    Optional<IdempotencyRecord> existingRecord = idempotencyService.checkIdempotency(idempotencyKey);
    if (existingRecord.isPresent() && existingRecord.get().isCompleted()) {
        return true;
    }
    return false;
}
```

##### B. Webhook/Callback Handler ⚠️ **CRITICAL**
**File**: `payments/services/PaymentService.java`

```java
public void handleZynlePayCallback(CardPaymentCallBackResponse callbackResponse) {
    // IDEMPOTENCY: Prevent duplicate webhook processing
    String idempotencyKey = "WEBHOOK_ZYNLEPAY_" + callbackResponse.getReferenceNo();
    
    Optional<IdempotencyRecord> existingRecord = idempotencyService.checkIdempotency(idempotencyKey);
    if (existingRecord.isPresent() && existingRecord.get().isCompleted()) {
        log.info("Webhook already processed. Skipping duplicate.");
        return; // Idempotent response
    }

    // Create idempotency record BEFORE processing
    idempotencyService.createIdempotencyRecord(
        idempotencyKey, "/callback/zynlepay", "POST", 
        objectMapper.writeValueAsString(callbackResponse), "zynlepay"
    );

    try {
        // Process webhook...
        processWebhook(callbackResponse);
        
        // Mark as completed
        idempotencyService.markAsCompleted(idempotencyKey, 200, "Processed", null);
    } catch (Exception e) {
        idempotencyService.markAsFailed(idempotencyKey, e.getMessage(), 500);
        throw e;
    }
}
```

**Why Critical**: Payment gateways retry webhooks. Without idempotency, duplicate credits/debits occur.

##### C. Payment Repository Enhancement
**File**: `payments/repositories/PaymentRepository.java`

```java
public interface PaymentRepository extends CrudRepository<Payment, Integer> {
    // For duplicate payment detection
    Optional<Payment> findByPaymentReference(String paymentReference);
}
```

---

### 2. Client Portal

**Location**: `webparent/client-portal/`

#### Transaction Processing ⚠️ **CRITICAL**
**File**: `transactions/controller/TransactionController.java`

```java
@PostMapping("/confirm")
public ResponseEntity<?> saveSubmittedPayment(
        @AuthenticationPrincipal ClientUserDetails loggedInUser, 
        @RequestBody Map<String, String> requestBody,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

    // Generate key if not provided (backwards compatibility)
    if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
        idempotencyKey = idempotencyService.generateIdempotencyKey();
    }

    // Check for duplicate
    Optional<IdempotencyRecord> existingRecord = idempotencyService.checkIdempotency(idempotencyKey);
    if (existingRecord.isPresent() && existingRecord.get().isCompleted()) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Collections.singletonMap("message", "Transaction already processed."));
    }

    // Create idempotency record
    idempotencyService.createIdempotencyRecord(
        idempotencyKey, "/transactions/confirm", "POST", 
        merchantCode + ":" + amount, loggedInUser.getClient().getEmail()
    );

    try {
        // Process transaction...
        Transaction trx = runPaymentSubmission(client, merchant, amount);
        updateClientBalance(amount, client, currentBalance);
        
        // Mark as completed
        idempotencyService.markAsCompleted(
            idempotencyKey, 200, 
            "{\"transactionRef\":\"" + trx.getReference() + "\"}", null
        );
        
        return ResponseEntity.ok(Collections.singletonMap("success", true));
    } catch (Exception e) {
        idempotencyService.markAsFailed(idempotencyKey, e.getMessage(), 500);
        throw e;
    }
}
```

**Why Critical**: Prevents duplicate charges when user clicks "Pay" multiple times or network retries occur.

---

### 3. Backoffice

**Location**: `webparent/backoffice/`

#### Voucher Approval Process
**File**: `voucher/controller/VoucherController.java`

**Implementation Status**: ⚠️ **Recommended**

```java
@GetMapping("/approve/{id}")
public String approveVoucherRequest(
        @PathVariable("id") Integer voucherId,
        HttpServletRequest request,
        RedirectAttributes redirectAttributes) throws PaykeshoError {
    
    // Generate idempotency key for this approval
    String idempotencyKey = "VOUCHER_APPROVAL_" + voucherId + "_" + System.currentTimeMillis();
    
    // Check if already approved
    Optional<IdempotencyRecord> existing = idempotencyService.checkIdempotency(idempotencyKey);
    if (existing.isPresent() && existing.get().isCompleted()) {
        redirectAttributes.addFlashAttribute("message", "Voucher already approved");
        return "redirect:/vouchers";
    }
    
    // Create idempotency record
    idempotencyService.createIdempotencyRecord(
        idempotencyKey, "/vouchers/approve/" + voucherId, "GET", 
        String.valueOf(voucherId), getCurrentUserEmail()
    );
    
    try {
        Voucher approvedVoucher = runVoucherApprovalProcess(voucherId, request);
        
        // Mark as completed
        idempotencyService.markAsCompleted(
            idempotencyKey, 200, 
            "{\"voucherRef\":\"" + approvedVoucher.getReference() + "\"}", null
        );
        
        redirectAttributes.addFlashAttribute("message", "Voucher approved: " + approvedVoucher.getReference());
        return "redirect:/vouchers";
    } catch (Exception e) {
        idempotencyService.markAsFailed(idempotencyKey, e.getMessage(), 500);
        throw e;
    }
}
```

---

### 4. Notification Service

**Location**: `webparent/notification-service/`

#### Duplicate Notification Prevention
**File**: `notifications/service/NotificationService.java`

**Implementation Status**: ⚠️ **Recommended**

```java
@Transactional
public boolean sendNotification(String recipient, String message, String notificationType) {
    // Create hash of notification content
    String contentHash = computeHash(recipient + message + notificationType);
    String idempotencyKey = "NOTIFICATION_" + contentHash;
    
    // Check if this notification was sent recently (within 1 hour)
    Optional<IdempotencyRecord> existing = idempotencyService.checkIdempotency(idempotencyKey);
    if (existing.isPresent() && existing.get().isCompleted()) {
        long timeSinceCreation = System.currentTimeMillis() - existing.get().getCreatedAt().getTime();
        if (timeSinceCreation < 60 * 60 * 1000) { // 1 hour
            log.info("Duplicate notification detected within 1 hour. Skipping.");
            return true; // Return true as it was already sent
        }
    }
    
    idempotencyService.createIdempotencyRecord(
        idempotencyKey, "/notifications/send", "POST", 
        contentHash, recipient
    );
    
    try {
        // Send notification...
        boolean result = actualSendNotification(recipient, message, notificationType);
        
        if (result) {
            idempotencyService.markAsCompleted(idempotencyKey, 200, "Sent", null);
        } else {
            idempotencyService.markAsFailed(idempotencyKey, "Send failed", 500);
        }
        
        return result;
    } catch (Exception e) {
        idempotencyService.markAsFailed(idempotencyKey, e.getMessage(), 500);
        throw e;
    }
}
```

---

## Critical Implementations

### 1. Balance Updates with Optimistic Locking

**Entity**: `commons/entity/client/ClientBalance.java`

```java
@Entity
@Table(name = "client_balance")
public class ClientBalance extends IdBasedEntity {
    
    @Column(name = "current_balance")
    private BigDecimal currentBalance;
    
    @Column(name = "available_balance")
    private BigDecimal availableBalance;
    
    /**
     * Optimistic locking version
     * Automatically prevents concurrent balance updates
     */
    @Version
    @Column(name = "version")
    private Long version;
}
```

**Usage in Service**:
```java
@Transactional
public void updateBalance(Client client, BigDecimal amount) {
    try {
        ClientBalance balance = clientBalanceRepository.findByClient(client);
        balance.setCurrentBalance(balance.getCurrentBalance().add(amount));
        balance.setAvailableBalance(balance.getAvailableBalance().add(amount));
        clientBalanceRepository.save(balance);
    } catch (OptimisticLockingFailureException e) {
        log.error("Concurrent balance update detected. Retrying...");
        // Retry logic or notify user
        throw new RuntimeException("Balance update conflict. Please try again.");
    }
}
```

---

### 2. Settlement Processing

**File**: `payment-service/settlement/service/SettlementService.java`

**Current Implementation**:
```java
@Transactional
public synchronized void reconcileAndSettleForMerchant(Merchant merchant) throws PaykeshoError {
    // synchronized prevents concurrent settlement in single instance
    // But NOT sufficient for distributed deployment
}
```

**Recommended Enhancement**:
```java
@Transactional
public void reconcileAndSettleForMerchant(Merchant merchant) throws PaykeshoError {
    String idempotencyKey = "SETTLEMENT_" + merchant.getId() + "_" + LocalDate.now();
    
    Optional<IdempotencyRecord> existing = idempotencyService.checkIdempotency(idempotencyKey);
    if (existing.isPresent() && existing.get().isCompleted()) {
        log.info("Settlement already processed for merchant {} today", merchant.getId());
        return;
    }
    
    idempotencyService.createIdempotencyRecord(
        idempotencyKey, "/settlements/process", "POST", 
        String.valueOf(merchant.getId()), "system"
    );
    
    try {
        List<Transaction> unsettledTransactions = 
            transactionRepository.findUnsettledTransactionsByMerchant(merchant);
        
        // Process settlement...
        Settlement settlement = createSettlement(merchant, unsettledTransactions);
        boolean success = initiatePayment(merchant, settlement);
        
        if (success) {
            markTransactionsAsSettled(unsettledTransactions);
            idempotencyService.markAsCompleted(
                idempotencyKey, 200, 
                "{\"settlementRef\":\"" + settlement.getReference() + "\"}", null
            );
        } else {
            idempotencyService.markAsFailed(idempotencyKey, "Payment initiation failed", 500);
        }
    } catch (Exception e) {
        idempotencyService.markAsFailed(idempotencyKey, e.getMessage(), 500);
        throw e;
    }
}
```

**Database Constraint** (Additional Safety):
```sql
-- Prevent duplicate settlements for same merchant on same day
ALTER TABLE settlements 
ADD UNIQUE INDEX uk_merchant_daily_settlement (
    merchant_id, 
    DATE(settlement_date), 
    settlement_status
);
```

---

## Testing Idempotency

### Manual Testing

#### 1. **Duplicate Payment Test**

```bash
# Send the same payment request twice with same idempotency key
curl -X POST http://localhost:8080/transactions/confirm \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-payment-12345" \
  -d '{
    "merchantCode": "MERCH001",
    "amount": "100.00"
  }'

# Expected: First request succeeds, second returns 409 Conflict
```

#### 2. **Webhook Replay Test**

```bash
# Send the same webhook twice
curl -X POST http://localhost:8080/rest/api/payments/callback/zynlepay \
  -H "Content-Type: application/json" \
  -d '{
    "referenceNo": "TXN123456",
    "responseCode": "100",
    "amount": "50.00"
  }'

# Expected: First creates payment, second logs "already processed" and returns 200
```

#### 3. **Concurrent Balance Update Test**

```java
@Test
public void testConcurrentBalanceUpdate() throws InterruptedException {
    Client client = createTestClient();
    
    // Simulate 5 concurrent balance updates
    ExecutorService executor = Executors.newFixedThreadPool(5);
    CountDownLatch latch = new CountDownLatch(5);
    
    for (int i = 0; i < 5; i++) {
        executor.submit(() -> {
            try {
                clientService.updateBalance(client, BigDecimal.valueOf(100));
            } catch (OptimisticLockingFailureException e) {
                // Expected for some threads
                log.info("Optimistic lock conflict detected (expected)");
            } finally {
                latch.countDown();
            }
        });
    }
    
    latch.await(10, TimeUnit.SECONDS);
    
    // Verify: Only successful updates are reflected in final balance
    ClientBalance finalBalance = clientService.getClientBalance(client);
    // Balance should be incremented by successful operations only
}
```

---

## Best Practices

### 1. **Always Provide Idempotency Keys for Financial Operations**

```javascript
// Frontend example
async function submitPayment(paymentData) {
    const idempotencyKey = generateUUID(); // or user-generated
    
    const response = await fetch('/transactions/confirm', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Idempotency-Key': idempotencyKey
        },
        body: JSON.stringify(paymentData)
    });
    
    if (response.status === 409) {
        alert('This payment has already been processed');
        return;
    }
    
    return await response.json();
}
```

### 2. **Idempotency Key Formats**

| Operation | Format | Example |
|-----------|--------|---------|
| User Payment | `PAYMENT_{userId}_{timestamp}` | `PAYMENT_1234_1699876543210` |
| Webhook | `WEBHOOK_{provider}_{reference}` | `WEBHOOK_ZYNLEPAY_TXN123` |
| Settlement | `SETTLEMENT_{merchantId}_{date}` | `SETTLEMENT_456_2024-01-15` |
| Notification | `NOTIFICATION_{contentHash}` | `NOTIFICATION_abc123def456` |
| Voucher Approval | `VOUCHER_APPROVAL_{voucherId}` | `VOUCHER_APPROVAL_789` |

### 3. **Error Handling**

```java
try {
    processPayment(request);
} catch (OptimisticLockingFailureException e) {
    // Concurrent update - retry or inform user
    log.warn("Concurrent operation detected. Please retry.");
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body("Please try again - concurrent operation detected");
} catch (DataIntegrityViolationException e) {
    // Unique constraint violation - likely duplicate
    log.error("Duplicate operation detected: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body("This operation has already been processed");
}
```

### 4. **Cleanup Scheduled Job**

```java
@Component
public class IdempotencyCleanupJob {
    
    @Autowired
    private IdempotencyService idempotencyService;
    
    /**
     * Clean up expired idempotency records daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredRecords() {
        log.info("Starting idempotency records cleanup");
        int deleted = idempotencyService.cleanupExpiredRecords();
        log.info("Cleaned up {} expired idempotency records", deleted);
        
        // Also cleanup stuck processing records
        int stuckCleaned = idempotencyService.cleanupStuckRecords();
        log.info("Cleaned up {} stuck processing records", stuckCleaned);
    }
}
```

---

## Troubleshooting

### Issue 1: "Idempotency record not found"

**Cause**: Record was cleaned up or never created.

**Solution**:
- Increase TTL for critical operations
- Ensure `createIdempotencyRecord()` is called before processing
- Check if cleanup job is running too frequently

### Issue 2: "Optimistic Locking Failure"

**Cause**: Multiple concurrent updates to same balance.

**Solution**:
- Implement retry logic with exponential backoff
- Use message queue for sequential processing
- Lock the account during processing

```java
@Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
public void updateBalanceWithRetry(Client client, BigDecimal amount) {
    updateBalance(client, amount);
}
```

### Issue 3: "Duplicate payments still occurring"

**Checklist**:
- [ ] Is idempotency key being sent from frontend?
- [ ] Is `checkIdempotency()` called before processing?
- [ ] Is database unique constraint on payment_reference enforced?
- [ ] Are all service instances using same database (not in-memory cache)?

### Issue 4: "Webhooks processed multiple times"

**Solution**:
- Verify idempotency check is at START of webhook handler
- Add database unique constraint on webhook reference
- Log all webhook attempts with idempotency status

---

## Monitoring and Metrics

### Key Metrics to Track

1. **Duplicate Request Rate**
   ```sql
   SELECT COUNT(*) as duplicate_attempts 
   FROM idempotency_records 
   WHERE attempt_count > 1 
   AND created_at > DATE_SUB(NOW(), INTERVAL 1 DAY);
   ```

2. **Failed Operations**
   ```sql
   SELECT endpoint, COUNT(*) as failures 
   FROM idempotency_records 
   WHERE status = 'FAILED' 
   AND created_at > DATE_SUB(NOW(), INTERVAL 1 DAY)
   GROUP BY endpoint;
   ```

3. **Stuck Operations**
   ```sql
   SELECT * FROM idempotency_records 
   WHERE status = 'PROCESSING' 
   AND created_at < DATE_SUB(NOW(), INTERVAL 1 HOUR);
   ```

---

## Summary

### ✅ Implemented

1. ✅ Idempotency infrastructure in commons
2. ✅ Database migration scripts
3. ✅ Idempotency service in all modules (decoupled)
4. ✅ Payment service database-backed idempotency
5. ✅ Payment webhook idempotency
6. ✅ Transaction processing idempotency
7. ✅ Optimistic locking on balances

### ⚠️ Recommended for Implementation

8. ⚠️ Notification service idempotency
9. ⚠️ Voucher approval idempotency
10. ⚠️ Settlement processing idempotency
11. ⚠️ Idempotency interceptor/filter for all REST APIs
12. ⚠️ Cleanup scheduled jobs

### 🔐 Security Enhancements

- Request hash validation prevents replay attacks
- Database constraints prevent duplicate business operations
- Optimistic locking prevents concurrent data corruption
- Automatic expiration prevents indefinite data growth

---

**Last Updated**: 2025-01-10
**Version**: 1.0
**Maintained By**: Paykesho Development Team


