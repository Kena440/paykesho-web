package org.paykesho.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.paykesho.paymentservice.repository.ProviderEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LencoWebhookEventServiceTest {
    @Mock ProviderEventRepository events;
    @Mock SettlementReconciliationService reconciliation;

    private LencoWebhookEventService service() {
        return new LencoWebhookEventService(events, reconciliation, new ObjectMapper());
    }

    @Test
    void exactWebhookRetryIsDeduplicatedAndDoesNotReconcileTwice() throws Exception {
        String payload = "{\"event\":\"transfer.failed\",\"data\":{\"reference\":\"PK-123\",\"status\":\"failed\"}}";
        when(events.insertIfAbsent(eq("LENCO"), anyString(), eq("transfer.failed"), eq("PK-123"), anyString(), eq(true)))
                .thenReturn(1, 0);

        assertTrue(service().accept(payload, "transfer.failed", "PK-123", "failed", "L-1", "declined"));
        assertFalse(service().accept(payload, "transfer.failed", "PK-123", "failed", "L-1", "declined"));

        verify(reconciliation, times(1)).applyLencoResult("PK-123", "failed", "L-1", "declined");
        verify(events, times(1)).markProcessed(eq("LENCO"), anyString());
    }

    @Test
    void storesOnlySanitizedFieldsNotRawProviderPayload() throws Exception {
        String payload = "{\"event\":\"transfer.successful\",\"data\":{\"reference\":\"PK-123\",\"status\":\"successful\",\"phone\":\"0977000000\",\"accountNumber\":\"123456789\"}}";
        when(events.insertIfAbsent(eq("LENCO"), anyString(), eq("transfer.successful"), eq("PK-123"), anyString(), eq(true)))
                .thenReturn(1);

        assertTrue(service().accept(payload, "transfer.successful", "PK-123", "successful", "L-2", null));

        ArgumentCaptor<String> id = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> safePayload = ArgumentCaptor.forClass(String.class);
        verify(events).insertIfAbsent(eq("LENCO"), id.capture(), eq("transfer.successful"), eq("PK-123"), safePayload.capture(), eq(true));
        assertEquals(64, id.getValue().length());
        assertFalse(safePayload.getValue().contains("0977000000"));
        assertFalse(safePayload.getValue().contains("123456789"));
        verify(reconciliation).applyLencoResult("PK-123", "successful", "L-2", null);
    }
}
