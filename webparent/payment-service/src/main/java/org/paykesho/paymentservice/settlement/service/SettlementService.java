package org.paykesho.paymentservice.settlement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.IdempotencyRecord;
import org.commons.entity.Transaction;
import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.MerchantMoneyAccountDetail;
import org.commons.entity.merchant.Settlement;
import org.commons.enums.SettlementStatus;
import org.commons.utils.PaykeshoError;
import org.paykesho.paymentservice.idempotency.service.IdempotencyService;
import org.paykesho.paymentservice.payments.dto.bank.BankPaymentRequest;
import org.paykesho.paymentservice.payments.dto.bank.BankPaymentResponse;
import org.paykesho.paymentservice.payments.dto.momo.MoMoPaymentToMerchantRequest;
import org.paykesho.paymentservice.payments.dto.momo.MoMoPaymentToMerchantResponse;
import org.paykesho.paymentservice.payments.services.PaymentService;
import org.paykesho.paymentservice.settlement.repository.MerchantMoneyAccountDetailsRepository;
import org.paykesho.paymentservice.settlement.repository.SettlementRepository;
import org.paykesho.paymentservice.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class SettlementService {

    private final PaymentService paymentService;
    private final TransactionRepository transactionRepository;
    private final SettlementRepository settlementRepository;
    private final MerchantMoneyAccountDetailsRepository merchantMoneyAccountDetailsRepository;
    private final IdempotencyService idempotencyService;

    @Transactional
    public void reconcileAndSettleForMerchant(Merchant merchant) throws PaykeshoError {
        if (merchant == null) {
            throw new PaykeshoError("Merchant cannot be null");
        }

        log.debug("Starting settlement process for merchant: {}", merchant.getId());

        // IDEMPOTENCY: Generate key for today's settlement
        String idempotencyKey = "SETTLEMENT_" + merchant.getId() + "_" + LocalDate.now();
        
        // Check if settlement already processed today
        Optional<IdempotencyRecord> existing = idempotencyService.checkIdempotency(idempotencyKey);
        if (existing.isPresent() && existing.get().isCompleted()) {
            log.info("Settlement already processed for merchant {} today. Skipping.", merchant.getId());
            return;
        }
        
        // If currently processing, don't start another
        if (existing.isPresent() && existing.get().isProcessing()) {
            long processingTime = System.currentTimeMillis() - existing.get().getCreatedAt().getTime();
            if (processingTime < 10 * 60 * 1000) { // 10 minutes
                log.warn("Settlement already in progress for merchant {}. Skipping.", merchant.getId());
                return;
            }
        }
        
        // Create idempotency record
        try {
            idempotencyService.createIdempotencyRecord(
                idempotencyKey,
                "/settlements/process",
                "POST",
                String.valueOf(merchant.getId()),
                "system"
            );
        } catch (Exception e) {
            log.error("Error creating idempotency record: {}", e.getMessage());
        }

        try {
            // Get unsettled transactions
            List<Transaction> unsettledTransactions = transactionRepository.findUnsettledTransactionsByMerchant(merchant);

            if (unsettledTransactions == null || unsettledTransactions.isEmpty()) {
                log.debug("No unsettled transactions found for merchant: {}", merchant.getId());
                idempotencyService.markAsCompleted(idempotencyKey, 200, "No transactions to settle", null);
                return;
            }

            // Validate all transactions are still unsettled (prevent race condition)
            List<Transaction> validUnsettledTransactions = unsettledTransactions.stream()
                    .filter(txn -> !txn.isSettled())
                    .collect(java.util.stream.Collectors.toList());

            if (validUnsettledTransactions.isEmpty()) {
                log.debug("All transactions already settled for merchant: {}", merchant.getId());
                idempotencyService.markAsCompleted(idempotencyKey, 200, "Transactions already settled", null);
                return;
            }

            BigDecimal totalAmount = validUnsettledTransactions.stream()
                    .map(Transaction::getAmount)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                String paykeshoRef = UUID.randomUUID().toString();
                if (paykeshoRef == null) throw new PaykeshoError("FAILED_GEN_PAYKESHO_REF");

                Settlement settlement = new Settlement();
                settlement.setReference(paykeshoRef);
                settlement.setMerchant(merchant);
                settlement.setSettlementAmount(totalAmount);
                settlement.setSettlementStatus(SettlementStatus.PENDING);
                settlementRepository.save(settlement);

                boolean paymentSuccessful = initiatePayment(merchant, settlement);

                if (paymentSuccessful) {
                    settlement.setSettlementStatus(SettlementStatus.COMPLETED);
                    markTransactionsAsSettled(unsettledTransactions);
                    
                    // Mark as completed
                    idempotencyService.markAsCompleted(
                        idempotencyKey, 200,
                        "{\"settlementRef\":\"" + settlement.getReference() + "\",\"amount\":\"" + totalAmount + "\"}", 
                        null
                    );
                } else {
                    settlement.setSettlementStatus(SettlementStatus.FAILED);
                    idempotencyService.markAsFailed(idempotencyKey, "Payment initiation failed", 500);
                }

                settlementRepository.save(settlement);
            } else {
                idempotencyService.markAsCompleted(idempotencyKey, 200, "No amount to settle", null);
            }
        } catch (Exception e) {
            log.error("Error during settlement processing: {}", e.getMessage());
            idempotencyService.markAsFailed(idempotencyKey, e.getMessage(), 500);
            throw e;
        }
    }

    private boolean initiatePayment(Merchant merchant, Settlement settlement) {
        try {
            MerchantMoneyAccountDetail detail = merchantMoneyAccountDetailsRepository.getMerchantMoneyAccountDetailByMerchantId(merchant.getId());

            if (detail == null) {
                log.error("Merchant account details not found for merchant ID: {}", merchant.getId());
                throw new PaykeshoError("Merchant money account details not found");
            }

            return switch (detail.getPaymentMethod()) {
                case MOBILE_MONEY -> processMoMoPayment(merchant, settlement, detail);
                case BANK_TRANSFER -> processBankPayment(merchant, settlement, detail);
                default -> {
                    log.error("Unsupported payment method: {}", detail.getPaymentMethod());
                    throw new PaykeshoError("Unsupported payment method");
                }
            };

        } catch (Exception e) {
            log.error("Payment initiation failed for merchant {}: {}", merchant.getId(), e.getMessage(), e);
            return false;
        }
    }

    private void markTransactionsAsSettled(List<Transaction> transactions) {
        transactions.forEach(txn -> txn.setSettled(true));
        transactionRepository.saveAll(transactions);
    }

    private boolean processMoMoPayment(Merchant merchant, Settlement settlement, MerchantMoneyAccountDetail detail) {
        try {
            MoMoPaymentToMerchantRequest request = new MoMoPaymentToMerchantRequest();
            request.setReferenceNo(settlement.getReference());
            request.setAmount(settlement.getSettlementAmount().toString());
            request.setReceiverId(detail.getAccountNumber());

            MoMoPaymentToMerchantResponse response = paymentService.runMoMoPaymentToMerchant(request);
            return isPaymentSuccessful(response != null ? response.getResponse().getResponseCode() : null);
        } catch (Exception e) {
            log.error("MoMo payment failed for merchant {}: {}", merchant.getId(), e.getMessage(), e);
            return false;
        }
    }

    private boolean processBankPayment(Merchant merchant, Settlement settlement, MerchantMoneyAccountDetail detail) {
        try {
            BankPaymentRequest request = new BankPaymentRequest();
            request.setReferenceNo(settlement.getReference());
            request.setAmount(settlement.getSettlementAmount().toString());
            request.setBankName(detail.getFinancialInstitution());
            request.setReceiverId(detail.getAccountNumber());

            BankPaymentResponse response = paymentService.runWalletToBankPayment(request);
            return isPaymentSuccessful(response != null ? response.getResponse_code() : null);
        } catch (Exception e) {
            log.error("Bank payment failed for merchant {}: {}", merchant.getId(), e.getMessage(), e);
            return false;
        }
    }

    private boolean isPaymentSuccessful(String responseCode) {
        if (responseCode == null) {
            return false;
        }

        return switch (responseCode) {
            case "100" -> // Successful
                    true; // Pending
            case "990", "120" -> {
                log.info("Payment is in progress: Response Code {}", responseCode);
                yield false;
            } // Failed
            // Merchant insufficient balance
            // Exceeds disbursement limit
            // Invalid bank name
            // Duplicate reference
            case "995", "9911", "9912", "9913", "9906", "9907" -> {
                log.warn("Payment failed due to: Response Code {}", responseCode);
                yield false;
            }
            default -> {
                log.error("Unexpected response code: {}", responseCode);
                yield false;
            }
        };
    }

}
