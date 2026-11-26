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
        when(reconciliation.applyLencoResult("PK-123", "failed", "L-1", "declined")).thenReturn(true);

        assertTrue(service().accept(payload, "transfer.failed", "PK-123", "failed", "L-1", "declined"));
        assertTrue(service().accept(payload, "transfer.failed", "PK-123", "failed", "L-1", "declined"));

        verify(reconciliation, times(1)).applyLencoResult("PK-123", "failed", "L-1", "declined");
        verify(events, times(1)).markProcessed(eq("LENCO"), anyString());
    }

    @Test
    void unprocessedEventRetriesWhenReferenceWasInitiallyUnknown() throws Exception {
        String payload = "{\"event\":\"transfer.failed\",\"data\":{\"reference\":\"PK-late\",\"status\":\"failed\"}}";
        org.commons.entity.ProviderEvent pending = new org.commons.entity.ProviderEvent();
        pending.setProvider("LENCO");
        pending.setEventId("digest");
        pending.setEventType("transfer.failed");
        pending.setProcessedAt(null);
        when(events.insertIfAbsent(eq("LENCO"), anyString(), eq("transfer.failed"), eq("PK-late"), anyString(), eq(true)))
                .thenReturn(1, 0);
        when(events.findByProviderAndEventId(eq("LENCO"), anyString())).thenReturn(java.util.Optional.of(pending));
        when(reconciliation.applyLencoResult("PK-late", "failed", null, null)).thenReturn(false, true);

        assertFalse(service().accept(payload, "transfer.failed", "PK-late", "failed", null, null));
        assertTrue(service().accept(payload, "transfer.failed", "PK-late", "failed", null, null));
        verify(reconciliation, times(2)).applyLencoResult("PK-late", "failed", null, null);
        verify(events, times(1)).markProcessed(eq("LENCO"), anyString());
    }

    @Test
    void storesOnlySanitizedFieldsNotRawProviderPayload() throws Exception {
        String payload = "{\"event\":\"transfer.successful\",\"data\":{\"reference\":\"PK-123\",\"status\":\"successful\",\"phone\":\"0977000000\",\"accountNumber\":\"123456789\"}}";
        when(events.insertIfAbsent(eq("LENCO"), anyString(), eq("transfer.successful"), eq("PK-123"), anyString(), eq(true)))
                .thenReturn(1);
        when(reconciliation.applyLencoResult("PK-123", "successful", "L-2", null)).thenReturn(true);

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
