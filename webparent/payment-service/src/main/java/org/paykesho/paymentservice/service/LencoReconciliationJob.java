package org.paykesho.paymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.Transaction;
import org.paykesho.paymentservice.dto.LencoSettlementResponse;
import org.paykesho.paymentservice.repository.TransactionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LencoReconciliationJob {

    private final TransactionRepository transactionRepository;
    private final LencoTransferService lencoTransferService;
    private final SettlementReconciliationService reconciliationService;

    @Scheduled(fixedDelayString = "${lenco.reconciliation.interval-ms:1800000}")
    public void reconcilePendingTransfers() {
        List<Transaction> pending = transactionRepository.findBySettlementStatus("PENDING");
        for (Transaction transaction : pending) {
            try {
                LencoSettlementResponse result = lencoTransferService.getStatus(transaction.getReference());
                if ("SUCCESSFUL".equalsIgnoreCase(result.getStatus()) || "FAILED".equalsIgnoreCase(result.getStatus())) {
                    reconciliationService.applyLencoResult(
                            transaction.getReference(),
                            result.getStatus(),
                            result.getLencoReference(),
                            result.getMessage()
                    );
                }
            } catch (Exception e) {
                log.warn("Pending Lenco reconciliation failed for {}: {}", transaction.getReference(), e.getMessage());
            }
        }
    }
}
