package org.paykesho.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.paykesho.paymentservice.service.LencoWebhookEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.HttpStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LencoWebhookControllerTest {
    @Mock LencoWebhookEventService eventService;

    private static final String TOKEN = "test-lenco-api-token";

    private LencoWebhookController controller() {
        LencoWebhookController controller = new LencoWebhookController(new ObjectMapper(), eventService);
        ReflectionTestUtils.setField(controller, "apiToken", TOKEN);
        return controller;
    }

    private static String signature(String payload) throws Exception {
        byte[] tokenHash = MessageDigest.getInstance("SHA-256").digest(TOKEN.getBytes(StandardCharsets.UTF_8));
        StringBuilder keyHex = new StringBuilder();
        for (byte b : tokenHash) keyHex.append(String.format("%02x", b));
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(keyHex.toString().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    @Test
    void forgedSignatureRejectedWithoutProcessing() {
        String payload = "{\"event\":\"transfer.successful\",\"data\":{\"reference\":\"PK-1\"}}";
        assertEquals(HttpStatus.UNAUTHORIZED, controller().receive("forged", payload).getStatusCode());
        verifyNoInteractions(eventService);
    }

    @Test
    void validSignatureProcessesSettlementEvent() throws Exception {
        String payload = "{\"event\":\"transfer.successful\",\"data\":{\"reference\":\"PK-1\",\"status\":\"successful\"}}";
        when(eventService.accept(payload, "transfer.successful", "PK-1", "successful", null, null)).thenReturn(true);

        assertEquals(HttpStatus.OK, controller().receive(signature(payload), payload).getStatusCode());
        verify(eventService).accept(payload, "transfer.successful", "PK-1", "successful", null, null);
    }

    @Test
    void validSignatureButUnknownReferenceIsRetried() throws Exception {
        String payload = "{\"event\":\"transfer.failed\",\"data\":{\"reference\":\"PK-late\",\"status\":\"failed\"}}";
        when(eventService.accept(payload, "transfer.failed", "PK-late", "failed", null, null)).thenReturn(false);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, controller().receive(signature(payload), payload).getStatusCode());
    }
}
