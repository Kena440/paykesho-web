package org.paykesho.clientportal.settlement;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.merchant.Merchant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class PaymentSettlementClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String paymentServiceUrl;
    private final String internalToken;

    public PaymentSettlementClient(
            @Value("${payment.service.url:http://payment-service:8084}") String paymentServiceUrl,
            @Value("${paykesho.internal.token:}") String internalToken) {
        this.paymentServiceUrl = paymentServiceUrl;
        this.internalToken = internalToken;
    }

    public String validateMerchantPayoutProfile(Merchant merchant) {
        if (merchant == null) return "Merchant payout profile is missing.";
        String method = merchant.getPayoutMethod();
        boolean hasRecipientId = merchant.getLencoTransferRecipientId() != null
                && !merchant.getLencoTransferRecipientId().trim().isEmpty();
        if ("mobile-money".equalsIgnoreCase(method)) {
            if (!hasRecipientId && (merchant.getPayoutOperator() == null || merchant.getPayoutOperator().trim().isEmpty())) {
                return "Merchant mobile-money operator is not configured.";
            }
            if (!hasRecipientId && (merchant.getPayoutPhone() == null || merchant.getPayoutPhone().trim().isEmpty())) {
                return "Merchant payout phone is not configured.";
            }
        } else if ("bank-account".equalsIgnoreCase(method)) {
            if (!hasRecipientId && (merchant.getPayoutBankId() == null || merchant.getPayoutBankId().trim().isEmpty()
                    || merchant.getPayoutAccountNumber() == null || merchant.getPayoutAccountNumber().trim().isEmpty())) {
                return "Merchant bank payout details are not configured.";
            }
        } else {
            return "Merchant payout method is not configured.";
        }
        return null;
    }

    public SettlementClientResponse settle(Merchant merchant, BigDecimal amount, String reference) {
        if (merchant == null || amount == null || reference == null) {
            return new SettlementClientResponse(false, "FAILED", "Invalid settlement details.", null);
        }
        if (internalToken == null || internalToken.trim().isEmpty()) {
            return new SettlementClientResponse(false, "FAILED", "Payment settlement service is not configured.", null);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amount);
        body.put("reference", reference);
        body.put("narration", "PayKesho merchant payment " + reference);
        body.put("payoutMethod", merchant.getPayoutMethod());
        body.put("transferRecipientId", merchant.getLencoTransferRecipientId());
        body.put("phone", merchant.getPayoutPhone() != null ? merchant.getPayoutPhone() : merchant.getPhoneNumber());
        body.put("operator", merchant.getPayoutOperator());
        body.put("country", "zm");
        body.put("bankId", merchant.getPayoutBankId());
        body.put("accountNumber", merchant.getPayoutAccountNumber());

        return post("/internal/v1/settlements", body);
    }

    public SettlementClientResponse reconcile(String reference, String status, String lencoReference, String reason) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reference", reference);
        body.put("status", status);
        body.put("lencoReference", lencoReference);
        body.put("reason", reason);
        return post("/internal/v1/settlements/reconcile", body);
    }

    private SettlementClientResponse post(String path, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-PayKesho-Internal-Token", internalToken);

            ResponseEntity<SettlementClientResponse> response = restTemplate.exchange(
                    paymentServiceUrl + path,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    SettlementClientResponse.class
            );

            SettlementClientResponse result = response.getBody();
            return result == null
                    ? new SettlementClientResponse(false, "FAILED", "Empty response from settlement service.", null)
                    : result;
        } catch (RestClientException e) {
            log.error("Payment settlement service call failed for {}: {}", path, e.getMessage());
            return new SettlementClientResponse(false, "FAILED", "Unable to reach payment settlement service.", null);
        }
    }
}
