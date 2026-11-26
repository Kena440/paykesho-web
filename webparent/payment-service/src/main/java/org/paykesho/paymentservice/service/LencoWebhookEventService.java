package org.paykesho.paymentservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.paykesho.paymentservice.repository.ProviderEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LencoWebhookEventService {
    private static final String PROVIDER = "LENCO";
    private final ProviderEventRepository events;
    private final SettlementReconciliationService reconciliationService;
    private final ObjectMapper objectMapper;

    /** Event digest deduplicates exact signed payload retries; insert and processing share one transaction. */
    @Transactional
    public boolean accept(String rawPayload, String eventType, String reference, String status,
                         String lencoReference, String reason) throws JsonProcessingException {
        String eventId = sha256(rawPayload);
        Map<String, Object> safePayload = new LinkedHashMap<>();
        safePayload.put("event", eventType);
        safePayload.put("reference", reference);
        safePayload.put("status", status);
        safePayload.put("lencoReference", lencoReference);
        safePayload.put("reasonForFailure", reason);
        String sanitized = objectMapper.writeValueAsString(safePayload);

        int inserted = events.insertIfAbsent(PROVIDER, eventId, eventType, reference, sanitized, true);
        if (inserted == 0) return false; // already received and processed, or concurrent delivery

        if ("transfer.successful".equals(eventType) || "transfer.failed".equals(eventType)) {
            reconciliationService.applyLencoResult(reference, status, lencoReference, reason);
        }
        events.markProcessed(PROVIDER, eventId);
        return true;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash provider event", e);
        }
    }
}
