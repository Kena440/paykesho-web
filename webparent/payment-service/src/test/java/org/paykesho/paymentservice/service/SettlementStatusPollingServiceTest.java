package org.paykesho.paymentservice.service;

import org.commons.entity.Transaction;
import org.paykesho.paymentservice.dto.LencoSettlementResponse;
import org.paykesho.paymentservice.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementStatusPollingServiceTest {
    @Mock TransactionRepository transactions;
    @Mock LencoTransferService lenco;
    @Mock SettlementReconciliationService reconciliation;

    @Test
    void pollsConfiguredProviderAndReconcilesKnownStatusesOnly() {
        Transaction successful = new Transaction();
        successful.setReference("PK-success");
        Transaction pending = new Transaction();
        pending.setReference("PK-pending");
        Transaction unknown = new Transaction();
        unknown.setReference("PK-unknown");

        when(lenco.isConfigured()).thenReturn(true);
        when(transactions.findBySettlementStatusAndSettlementUpdatedAtBefore(eq("PENDING"), any(), any()))
                .thenReturn(List.of(successful, pending, unknown));
        when(lenco.getStatus("PK-success")).thenReturn(new LencoSettlementResponse(true, "SUCCESSFUL", "done", "L-1"));
        when(lenco.getStatus("PK-pending")).thenReturn(new LencoSettlementResponse(true, "PENDING", "still processing", null));
        when(lenco.getStatus("PK-unknown")).thenReturn(new LencoSettlementResponse(false, "UNKNOWN", "auth unavailable", null));

        new SettlementStatusPollingService(transactions, lenco, reconciliation).reconcileStalePendingSettlements();

        verify(reconciliation).applyLencoResult("PK-success", "SUCCESSFUL", "L-1", "done");
        verify(reconciliation).applyLencoResult("PK-pending", "PENDING", null, "still processing");
        verify(reconciliation, never()).applyLencoResult(eq("PK-unknown"), anyString(), any(), any());
    }

    @Test
    void skipsPollingWhenProviderIsNotConfigured() {
        when(lenco.isConfigured()).thenReturn(false);
        new SettlementStatusPollingService(transactions, lenco, reconciliation).reconcileStalePendingSettlements();
        verifyNoInteractions(transactions, reconciliation);
    }
}
