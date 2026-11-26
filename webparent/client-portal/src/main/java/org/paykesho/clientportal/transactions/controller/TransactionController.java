package org.paykesho.clientportal.transactions.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.Transaction;
import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.entity.client.ClientBalance;
import org.commons.entity.merchant.Merchant;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.api.dto.MerchantDto;
import org.paykesho.clientportal.api.dto.PaymentDto;
import org.paykesho.clientportal.clients.service.ClientService;
import org.paykesho.clientportal.merchant.service.MerchantService;
import org.paykesho.clientportal.notification.general.NotificationClient;
import org.paykesho.clientportal.security.ClientUserDetails;
import org.paykesho.clientportal.transactions.service.TransactionsService;
import org.paykesho.clientportal.utils.PaykeshoReferenceService;
import org.paykesho.clientportal.voucher.service.VoucherService;
import org.paykesho.clientportal.idempotency.service.IdempotencyService;
import org.paykesho.clientportal.transactions.pin.TransactionPinService;
import org.paykesho.clientportal.settlement.PaymentSettlementClient;
import org.paykesho.clientportal.settlement.SettlementClientResponse;
import org.commons.entity.IdempotencyRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
    private ClientService clientService;
    @Autowired
    private VoucherService voucherService;
    @Autowired
    private MerchantService merchantService;
    @Autowired
    private PaykeshoReferenceService paykeshoReferenceService;
    @Autowired
    private TransactionsService transactionsService;
    @Autowired
    private NotificationClient notificationClient;
    @Autowired
    private IdempotencyService idempotencyService;
    @Autowired
    private TransactionPinService transactionPinService;
    @Autowired
    private PaymentSettlementClient paymentSettlementClient;

    @GetMapping("")
    public String showClientTransactions(@AuthenticationPrincipal ClientUserDetails loggedInUser, Model model) throws PaykeshoError {
        Client client = clientService.getClientByEmail(loggedInUser.getClient().getEmail());
        List<Transaction> transactionList = transactionsService.getClientTransactions(client);
        model.addAttribute("customer", client);
        model.addAttribute("transactionList", transactionList);
        model.addAttribute("pageTitle", "My Transactions");

        return "transactions/transactions";
    }

    @GetMapping("/initiate")
    public String showClientTransactionPaymentForm(@AuthenticationPrincipal ClientUserDetails loggedInUser, Model model) throws PaykeshoError {
        Client client = clientService.getClientByEmail(loggedInUser.getClient().getEmail());
        List<Voucher> voucherList = voucherService.getApprovedAndValidClientVouchers(client);
        model.addAttribute("voucherList", voucherList);
        model.addAttribute("paymentDto", new PaymentDto());
        model.addAttribute("pageTitle", "Initiate Transaction");

        return "transactions/initiate";
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> saveSubmittedPayment(
            @AuthenticationPrincipal ClientUserDetails loggedInUser, 
            @RequestBody Map<String, String> requestBody,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        // Money-moving requests must supply a stable idempotency key. Never generate
        // one server-side: a retry with a new key could create a second payment.
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()
                || idempotencyKey.length() > 255) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", "A valid Idempotency-Key is required."));
        }
        idempotencyKey = idempotencyKey.trim();

        String merchantCode = requestBody.get("merchantCode");
        if (merchantCode == null || merchantCode.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", "Merchant code is required."));
        }
        merchantCode = merchantCode.trim();

        BigDecimal amount;
        try {
            amount = new BigDecimal(requestBody.get("amount"));
        } catch (NumberFormatException | NullPointerException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "Invalid amount format. Please enter a valid number."));
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "Payment amount must be greater than zero."));
        }

        // Validate the transaction PIN before reserving a payment idempotency key.
        // PIN attempts are persisted and locked after three failures.
        TransactionPinService.Result pinResult = transactionPinService.verify(
                loggedInUser.getClient().getId(), requestBody.get("pin"));
        if (pinResult != TransactionPinService.Result.OK) {
            HttpStatus pinStatus = pinResult == TransactionPinService.Result.NOT_SET ? HttpStatus.PRECONDITION_REQUIRED
                    : pinResult == TransactionPinService.Result.LOCKED ? HttpStatus.LOCKED
                    : pinResult == TransactionPinService.Result.WRONG ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
            String pinMessage = pinResult == TransactionPinService.Result.NOT_SET ? "Set your transaction PIN before making payments."
                    : pinResult == TransactionPinService.Result.LOCKED ? "Transaction PIN is locked. Account recovery is required."
                    : pinResult == TransactionPinService.Result.WRONG ? "Incorrect transaction PIN."
                    : "Enter a valid six-digit transaction PIN.";
            return ResponseEntity.status(pinStatus).body(Collections.singletonMap("message", pinMessage));
        }

        String requestFingerprint = merchantCode + ":" + amount.stripTrailingZeros().toPlainString();
        String userIdentifier = loggedInUser.getClient().getEmail();

        // Bind a key to exactly one user and one normalized request. A reused key
        // with a different payload must never be allowed to proceed.
        Optional<IdempotencyRecord> existingRecord = idempotencyService.checkIdempotency(idempotencyKey);
        if (existingRecord.isPresent()) {
            IdempotencyRecord record = existingRecord.get();
            if (!userIdentifier.equals(record.getUserIdentifier())
                    || !idempotencyService.validateRequestHash(record, requestFingerprint)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Collections.singletonMap("message", "This idempotency key is already bound to a different request."));
            }
            if (record.isCompleted()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Collections.singletonMap("message", "Transaction already processed. Please check your transaction history."));
            }
            if ("PROCESSING".equalsIgnoreCase(record.getStatus())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Collections.singletonMap("message", "This payment is already being processed. Please wait for the result."));
            }
            if ("FAILED".equalsIgnoreCase(record.getStatus())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Collections.singletonMap("message", "This payment attempt previously failed. Please start a new payment attempt."));
            }
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Collections.singletonMap("message", "This payment key cannot be reused."));
        }

        // Fail closed if the idempotency record cannot be created. The prior
        // implementation swallowed persistence errors and continued to settlement.
        String finalIdempotencyKey = idempotencyKey;
        try {
            idempotencyService.createIdempotencyRecord(
                finalIdempotencyKey,
                "/transactions/confirm",
                "POST",
                requestFingerprint,
                userIdentifier
            );
        } catch (Exception e) {
            log.error("Unable to reserve payment idempotency key; refusing payment.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Collections.singletonMap("message", "Payment could not be safely started. Please retry."));
        }

        try {
            Client client = clientService.getClientByEmail(loggedInUser.getClient().getEmail());

            MerchantDto merchant = merchantService.getMerchantByCode(merchantCode);
            if (merchant == null) {
                idempotencyService.markAsFailed(finalIdempotencyKey, "Invalid merchant code", 400);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "Invalid merchant code."));
            }

        Voucher voucher = voucherService.getApprovedAndValidClientVouchers(client).stream()
                    .filter(v -> v.getAvailableAmount() != null && v.getAvailableAmount().compareTo(amount) >= 0)
                    .findFirst()
                    .orElse(null);

            if (voucher == null) {
                idempotencyService.markAsFailed(finalIdempotencyKey, "Insufficient e-value", 400);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "Insufficient PayKesho e-value."));
            }

            Transaction trx = runPaymentSubmission(client, merchant, voucher, amount);

            if (trx != null && trx.getReference() != null && !trx.getReference().isEmpty()) {
                ResponseEntity<?> response = updateClientBalance(client, trx);
                
                // Mark as completed after successful transaction
                idempotencyService.markAsCompleted(finalIdempotencyKey, 200, "{\"transactionRef\":\"" + trx.getReference() + "\"}", null);
                
                return response;
            } else {
                idempotencyService.markAsFailed(finalIdempotencyKey, "Failed to create transaction reference", 500);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("message", "Payment failed. Could not create transaction reference."));
            }
        } catch (PaykeshoError e) {
            log.error("Paykesho Error during payment submission: " + e.getMessage());
            idempotencyService.markAsFailed(finalIdempotencyKey, e.getMessage(), 500);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("message", "Payment processing failed: " + e.getMessage()));
        } catch (Exception e) {
            log.error("An unexpected error occurred during payment submission:", e);
            idempotencyService.markAsFailed(finalIdempotencyKey, e.getMessage(), 500);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("message", "An unexpected error occurred while processing your payment."));
        }
    }

    private ResponseEntity<Map<String, Object>> updateClientBalance(Client client, Transaction transaction) throws PaykeshoError {
        ClientBalance clientBalance = clientService.getClientBalance(client);
        if (clientBalance == null) {
            clientBalance = new ClientBalance();
            clientBalance.setClient(client);
            clientBalance.setTotalCredits(BigDecimal.ZERO);
            clientBalance.setTotalDebits(BigDecimal.ZERO);
            clientBalance.setHeldBalance(BigDecimal.ZERO);
        }

        BigDecimal remaining = voucherService.getApprovedAndValidClientVouchersBalance(client);
        clientBalance.setCurrentBalance(remaining);
        clientBalance.setAvailableBalance(remaining);
        clientBalance.setTotalDebits(
                clientBalance.getTotalDebits() == null ? BigDecimal.ZERO : clientBalance.getTotalDebits()
        );
        clientBalance.setLastUpdate(new Date());
        clientBalance.setLastTransactionId(transaction.getReference());
        clientBalance.setLastTransactionType("PAYMENT");
        clientBalance.setLastTransactionAmount(transaction.getAmount());
        clientService.saveClientBalance(clientBalance);

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("success", true);
        response.put("transactionRef", transaction.getReference());
        response.put("merchant", transaction.getMerchant().getName());
        response.put("amount", transaction.getAmount());
        response.put("remainingBalance", remaining);
        response.put("settled", transaction.isSettled());
        response.put("settlementStatus", transaction.getSettlementStatus());
        response.put("settlementReference", transaction.getSettlementReference());
        return ResponseEntity.ok(response);
    }

    private Transaction runPaymentSubmission(Client client, MerchantDto merchantDto, Voucher voucher, BigDecimal amount) throws PaykeshoError {

        String paykeshoRef = paykeshoReferenceService.generateCode("ITEM_REQ_TRX");
        if (paykeshoRef == null) throw new PaykeshoError("Failed to generate transaction reference.");

        Transaction transaction = new Transaction();
        transaction.setClient(client);
        Merchant merchant = merchantService.getMerchantByPaykeshoRef(merchantDto.getCode());
        if (merchant == null) {
            throw new PaykeshoError("Could not find merchant with the provided code.");
        }

        String payoutValidationError = paymentSettlementClient.validateMerchantPayoutProfile(merchant);
        if (payoutValidationError != null) {
            throw new PaykeshoError(payoutValidationError);
        }

        transaction.setMerchant(merchant);
        transaction.setAmount(amount);
        transaction.setReference(paykeshoRef);
        transaction.setNaration(paykeshoRef);
        transaction.setTxnDate(new Date());
        transaction.setCurrency("ZMW");
        transaction.setSettled(false);
        transaction.setSettlementStatus("PENDING");

        Transaction savedTxn;
        try {
            savedTxn = transactionsService.savePaymentAndConsumeVoucher(transaction, voucher.getId(), amount);
        } catch (Exception e) {
            log.error("Error saving transaction:", e);
            throw new PaykeshoError("Failed to save transaction: " + e.getMessage());
        }

        // The PayKesho ledger is debited first; Lenco is the external settlement rail.
        // The transaction remains PENDING until Lenco confirms the payout.
        SettlementClientResponse settlement = paymentSettlementClient.settle(merchant, amount, paykeshoRef);
        savedTxn.setSettlementStatus(settlement.getStatus());
        savedTxn.setSettlementReference(settlement.getLencoReference());
        savedTxn.setSettlementReason(settlement.getMessage());
        savedTxn.setSettlementUpdatedAt(new Date());

        if ("SUCCESSFUL".equalsIgnoreCase(settlement.getStatus())) {
            savedTxn.setSettled(true);
        } else if ("FAILED".equalsIgnoreCase(settlement.getStatus())) {
            savedTxn.setSettled(false);
            try {
                SettlementClientResponse reconciliation = paymentSettlementClient.reconcile(
                        paykeshoRef, "FAILED", settlement.getLencoReference(), settlement.getMessage());
                if (!reconciliation.isAccepted()) {
                    transactionsService.refundFailedSettlement(savedTxn);
                }
            } catch (Exception reconcileError) {
                log.error("Failed to reconcile failed Lenco payout {}: {}", paykeshoRef, reconcileError.getMessage());
                transactionsService.refundFailedSettlement(savedTxn);
            }
            throw new PaykeshoError("Merchant settlement failed: " + settlement.getMessage());
        } else {
            savedTxn.setSettled(false);
        }

        savedTxn = transactionsService.save(savedTxn);
        sendNotification(savedTxn);
        return savedTxn;
    }

    private void sendNotification(Transaction transaction) {
        if (transaction == null || transaction.getClient() == null || transaction.getMerchant() == null) {
            log.error("Cannot send notification: Transaction or related details are null.");
            return;
        }

        try {
            // Send email notification using professional template
            boolean emailSent = notificationClient.sendTransactionNotificationEmail(
                transaction.getClient().getFullName(),
                transaction.getClient().getEmail(),
                transaction.getReference(),
                transaction.getMerchant().getName(),
                transaction.getAmount()
            );
            
            // Generate SMS message
            String smsMessage = generateSmsMessage(transaction, "PAYMENT");
            
            if (emailSent) {
                log.info("Transaction email notification request created for: {}", transaction.getClient().getEmail());
            } else {
                log.error("Failed to create email notification request for: {}", transaction.getClient().getEmail());
            }

            // Notify merchant immediately for verification/receipt.
            if (transaction.getMerchant().getPhoneNumber() != null && !transaction.getMerchant().getPhoneNumber().trim().isEmpty()) {
                String merchantSms = String.format(
                    "PAYKESHO PAYMENT %s: K%s from %s. Reference: %s. Merchant: %s.",

                    transaction.isSettled() ? "RECEIVED" : "INITIATED",
                    transaction.getAmount(),
                    transaction.getClient().getFullName(),
                    transaction.getReference(),
                    transaction.getMerchant().getName()
                );
                notificationClient.sendNotification(transaction.getMerchant().getPhoneNumber(), merchantSms, "sms");
            }
            if (transaction.getMerchant().getEmail() != null && !transaction.getMerchant().getEmail().trim().isEmpty()) {
                String merchantEmail = String.format(
                    "PayKesho payment %s: K%s from %s. Reference: %s. Merchant: %s.",
                    transaction.isSettled() ? "RECEIVED" : "INITIATED",
                    transaction.getAmount(),
                    transaction.getClient().getFullName(),
                    transaction.getReference(),
                    transaction.getMerchant().getName()
                );
                notificationClient.sendNotification(transaction.getMerchant().getEmail(), merchantEmail, "email");
            }

            // Send SMS notification via NotificationClient
            if (transaction.getClient().getPhoneNumber() != null && !transaction.getClient().getPhoneNumber().trim().isEmpty()) {
                boolean smsSent = notificationClient.sendNotification(
                    transaction.getClient().getPhoneNumber(), 
                    smsMessage, 
                    "sms"
                );
                
                if (smsSent) {
                    log.info("Transaction SMS notification request created for: {}", transaction.getClient().getPhoneNumber());
                } else {
                    log.error("Failed to create SMS notification request for: {}", transaction.getClient().getPhoneNumber());
                }
            } else {
                log.warn("No phone number available for SMS notification to client: {}", transaction.getClient().getEmail());
            }
            
        } catch (Exception e) {
            log.error("Error creating notification requests for transaction {}: {}", 
                     transaction.getReference(), e.getMessage());
        }
    }

    private String generateEmailMessage(Transaction transaction, String actionType) {
        if (transaction == null || transaction.getClient() == null || transaction.getMerchant() == null) {
            log.error("Cannot generate email message: Transaction or related details are null.");
            return "";
        }

        StringBuilder message = new StringBuilder("<p>Dear ").append(transaction.getClient().getFullName()).append(",</p>");

        if ("PAYMENT".equals(actionType)) {
            message.append("<p>Your payment request has been processed successfully. Transaction Amount: K").append(transaction.getAmount()).append(" Merchant: ").append(transaction.getMerchant().getName()).append(" Transaction Reference: ").append(transaction.getReference()).append(".</p>");
        } else {
            message.append("<p>Unknown action type.</p>");
        }

        message.append("<br><p>Thank you,</p><p>Paykesho Team</p>");
        return message.toString();
    }

    private String generateSmsMessage(Transaction transaction, String actionType) {
        if (transaction == null || transaction.getClient() == null || transaction.getMerchant() == null) {
            log.error("Cannot generate SMS message: Transaction or related details are null.");
            return "Unknown action type.";
        }

        String txnAmount = "K" + transaction.getAmount();
        if ("PAYMENT".equals(actionType)) {
            String clientName = transaction.getClient().getFullName() != null ? transaction.getClient().getFullName() : "Client";
            String statusText = transaction.isSettled()
                    ? "was completed successfully"
                    : "was initiated and is awaiting merchant settlement";
            return String.format("Dear %s, your PayKesho payment %s. Amount: %s. Merchant: %s. Reference: %s.",
                    clientName, statusText, txnAmount, transaction.getMerchant().getName(), transaction.getReference());
        }
        return "Unknown action type.";
    }
}
