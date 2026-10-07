package org.paykesho.paymentservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.paykesho.paymentservice.service.SettlementReconciliationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/webhooks/lenco")
public class LencoWebhookController {

    private final ObjectMapper objectMapper;
    private final SettlementReconciliationService reconciliationService;

    @Value("${lenco.api.token:}")
    private String apiToken;

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestHeader(value = "X-Lenco-Signature", required = false) String signature,
            @RequestBody String payload) {

        if (!isValidSignature(signature, payload)) {
            return ResponseEntity.status(401).build();
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asText("");
            JsonNode data = root.path("data");

            if ("transfer.successful".equals(event) || "transfer.failed".equals(event)) {
                reconciliationService.applyLencoResult(
                        data.path("reference").asText(null),
                        data.path("status").asText("PENDING"),
                        data.path("lencoReference").asText(null),
                        data.path("reasonForFailure").asText(null)
                );
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private boolean isValidSignature(String signature, String payload) {
        if (signature == null || signature.trim().isEmpty() || apiToken == null || apiToken.trim().isEmpty()) {
            return false;
        }

        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256.digest(apiToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder webhookKeyHex = new StringBuilder();
            for (byte b : digest) {
                webhookKeyHex.append(String.format("%02x", b));
            }

            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(webhookKeyHex.toString().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] expected = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : expected) {
                hex.append(String.format("%02x", b));
            }

            return MessageDigest.isEqual(
                    hex.toString().getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            return false;
        }
    }
}
