package org.paykesho.paymentservice.settlement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.Transaction;
import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.MerchantMoneyAccountDetail;
import org.commons.entity.merchant.Settlement;
import org.commons.enums.SettlementStatus;
import org.commons.utils.PaykeshoError;
import org.paykesho.paymentservice.payments.dto.bank.BankPaymentRequest;
import org.paykesho.paymentservice.payments.dto.bank.BankPaymentResponse;
import org.paykesho.paymentservice.payments.dto.momo.MoMoPaymentRequest;
import org.paykesho.paymentservice.payments.dto.momo.MoMoPaymentResponse;
import org.paykesho.paymentservice.payments.services.PaymentService;
import org.paykesho.paymentservice.settlement.repository.MerchantMoneyAccountDetailsRepository;
import org.paykesho.paymentservice.settlement.repository.SettlementRepository;
import org.paykesho.paymentservice.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class SettlementService {

    private final PaymentService paymentService;
    private final TransactionRepository transactionRepository;
    private final SettlementRepository settlementRepository;
    private final MerchantMoneyAccountDetailsRepository merchantMoneyAccountDetailsRepository;

    public void reconcileAndSettleForMerchant(Merchant merchant) throws PaykeshoError {
        List<Transaction> unsettledTransactions = transactionRepository.findUnsettledTransactionsByMerchant(merchant);
        BigDecimal totalAmount = unsettledTransactions.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

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
            } else {
                settlement.setSettlementStatus(SettlementStatus.FAILED);
            }

            settlementRepository.save(settlement);
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
            MoMoPaymentRequest request = new MoMoPaymentRequest();
            request.setReferenceNo(settlement.getReference());
            request.setAmount(settlement.getSettlementAmount().toString());
            request.setReceiverId(detail.getAccountNumber());

            MoMoPaymentResponse response = paymentService.runMoMoPayment(request);
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
