package org.paykesho.paymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.Transaction;
import org.commons.entity.Voucher;
import org.paykesho.paymentservice.repository.TransactionRepository;
import org.paykesho.paymentservice.repository.VoucherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementReconciliationService {

    private final TransactionRepository transactionRepository;
    private final VoucherRepository voucherRepository;

    @Transactional
    public void applyLencoResult(String reference, String status, String lencoReference, String reason) {
        if (reference == null || reference.trim().isEmpty()) {
            return;
        }

        Transaction transaction = transactionRepository.findByReferenceForUpdate(reference).orElse(null);
        if (transaction == null) {
            log.warn("Received Lenco settlement event for unknown PayKesho reference {}", reference);
            return;
        }

        String normalized = status == null ? "PENDING" : status.toUpperCase();
        String current = transaction.getSettlementStatus() == null
                ? "PENDING"
                : transaction.getSettlementStatus().toUpperCase();

        // Terminal states are immutable. This prevents a duplicate or out-of-order
        // webhook from refunding an already successful payment or marking a refunded
        // payment successful after the e-value has been released.
        if ("SUCCESSFUL".equals(current) && "FAILED".equals(normalized)) {
            log.warn("Ignoring late FAILED settlement event for already successful transaction {}", reference);
            return;
        }
        if ("FAILED".equals(current) && "SUCCESSFUL".equals(normalized)) {
            log.error("Ignoring late SUCCESSFUL settlement event for already refunded transaction {}. Manual reconciliation required.", reference);
            return;
        }

        if ("SUCCESSFUL".equals(normalized)) {
            transaction.setSettled(true);
            transaction.setSettlementStatus("SUCCESSFUL");
            transaction.setSettlementReference(lencoReference);
            transaction.setSettlementReason(null);
            transaction.setSettlementUpdatedAt(new Date());
            transactionRepository.save(transaction);
            return;
        }

        if ("FAILED".equals(normalized)) {
            // A failed payout must release the e-value consumed for this payment.
            // Guard on settlement status so a repeated webhook cannot refund twice.
            if (!"FAILED".equalsIgnoreCase(transaction.getSettlementStatus())
                    && !"SUCCESSFUL".equalsIgnoreCase(transaction.getSettlementStatus())
                    && transaction.getVoucher() != null) {
                Voucher voucher = transaction.getVoucher();
                if (voucher.getAvailableAmount() == null) {
                    voucher.setAvailableAmount(transaction.getAmount());
                } else {
                    voucher.setAvailableAmount(voucher.getAvailableAmount().add(transaction.getAmount()));
                }
                voucherRepository.save(voucher);
            }
            transaction.setSettled(false);
            transaction.setSettlementStatus("FAILED");
            transaction.setSettlementReference(lencoReference);
            transaction.setSettlementReason(reason);
            transaction.setSettlementUpdatedAt(new Date());
            transactionRepository.save(transaction);
            return;
        }

        transaction.setSettlementStatus("PENDING");
        transaction.setSettlementReference(lencoReference);
        transaction.setSettlementReason(reason);
        transaction.setSettlementUpdatedAt(new Date());
        transactionRepository.save(transaction);
    }
}
