package org.paykesho.paymentservice.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.Transaction;
import org.paykesho.paymentservice.dto.LencoSettlementResponse;
import org.paykesho.paymentservice.repository.TransactionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/** Polls stale pending payouts so lost webhooks/timeouts do not leave settlements unresolved. */
@Slf4j
@Service
@ConditionalOnProperty(name = "paykesho.settlement.reconciliation.enabled", havingValue = "true", matchIfMissing = true)
public class SettlementStatusPollingService {
    private static final long STALE_AFTER_MS = 30L * 60L * 1000L;
    private static final int BATCH_SIZE = 50;

    private final TransactionRepository transactions;
    private final LencoTransferService lenco;
    private final SettlementReconciliationService reconciliation;

    public SettlementStatusPollingService(TransactionRepository transactions,
                                          LencoTransferService lenco,
                                          SettlementReconciliationService reconciliation) {
        this.transactions = transactions;
        this.lenco = lenco;
        this.reconciliation = reconciliation;
    }

    @Scheduled(fixedDelayString = "${paykesho.settlement.reconciliation.delay-ms:1800000}")
    public void reconcileStalePendingSettlements() {
        if (!lenco.isConfigured()) return;
        Date cutoff = new Date(System.currentTimeMillis() - STALE_AFTER_MS);
        List<Transaction> pending = transactions.findBySettlementStatusAndSettlementUpdatedAtBefore(
                "PENDING", cutoff, PageRequest.of(0, BATCH_SIZE, Sort.by(Sort.Direction.ASC, "settlementUpdatedAt")));
        for (Transaction transaction : pending) {
            try {
                LencoSettlementResponse result = lenco.getStatus(transaction.getReference());
                if (result == null || "UNKNOWN".equalsIgnoreCase(result.getStatus())) {
                    log.warn("Lenco status remains unknown for pending PayKesho reference {}", transaction.getReference());
                    continue;
                }
                reconciliation.applyLencoResult(
                        transaction.getReference(), result.getStatus(), result.getLencoReference(), result.getMessage());
            } catch (Exception e) {
                // One provider/network failure must not stop the rest of the batch.
                log.warn("Settlement status poll failed for reference {}: {}", transaction.getReference(), e.getMessage());
            }
        }
    }
}
