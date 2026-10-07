package org.paykesho.paymentservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.paykesho.paymentservice.dto.LencoSettlementRequest;
import org.paykesho.paymentservice.dto.LencoSettlementResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class LencoTransferService {

    private final WebClient webClient;
    private final String accountId;
    private final String apiToken;

    public LencoTransferService(
            @Value("${lenco.api.base-url:https://api.lenco.co/access/v2}") String baseUrl,
            @Value("${lenco.api.token:}") String apiToken,
            @Value("${lenco.api.account-id:}") String accountId) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.apiToken = apiToken;
        this.accountId = accountId;
    }

    public LencoSettlementResponse getStatus(String reference) {
        if (apiToken == null || apiToken.trim().isEmpty() || reference == null || reference.trim().isEmpty()) {
            return new LencoSettlementResponse(false, "FAILED", "Lenco status lookup is not configured.", null);
        }
        try {
            JsonNode root = webClient.get()
                    .uri("/transfers/status/" + reference)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            JsonNode data = root == null ? null : root.path("data");
            String status = data == null ? "FAILED" : data.path("status").asText("FAILED").toUpperCase();
            String message = root == null ? "Empty response from Lenco." : root.path("message").asText("");
            String lencoReference = data == null ? null : data.path("lencoReference").asText(null);
            return new LencoSettlementResponse(
                    "PENDING".equals(status) || "SUCCESSFUL".equals(status),
                    status,
                    message,
                    lencoReference
            );
        } catch (WebClientResponseException e) {
            log.warn("Lenco transfer status lookup failed for {}: {}", reference, e.getStatusCode());
            if (e.getStatusCode().value() == 401) {
                return new LencoSettlementResponse(false, "FAILED", "Lenco authorization failed.", null);
            }
            return new LencoSettlementResponse(true, "PENDING", "Status lookup temporarily unavailable.", null);
        } catch (Exception e) {
            log.warn("Unable to re-query Lenco transfer {}: {}", reference, e.getMessage());
            return new LencoSettlementResponse(true, "PENDING", "Status lookup temporarily unavailable.", null);
        }
    }

    public LencoSettlementResponse initiate(LencoSettlementRequest request) {
        if (apiToken == null || apiToken.trim().isEmpty() || accountId == null || accountId.trim().isEmpty()) {
            return new LencoSettlementResponse(false, "FAILED", "Lenco is not configured.", null);
        }
        if (request == null || request.getAmount() == null || request.getReference() == null
                || request.getReference().trim().isEmpty()) {
            return new LencoSettlementResponse(false, "FAILED", "Invalid settlement request.", null);
        }

        String method = request.getPayoutMethod() == null ? "mobile-money" : request.getPayoutMethod();
        String path;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accountId", accountId);
        body.put("amount", request.getAmount());
        body.put("narration", request.getNarration() == null ? "PayKesho merchant payment" : request.getNarration());
        body.put("reference", request.getReference());

        if ("bank-account".equalsIgnoreCase(method)) {
            path = "/transfers/bank-account";
            if (request.getTransferRecipientId() != null && !request.getTransferRecipientId().trim().isEmpty()) {
                body.put("transferRecipientId", request.getTransferRecipientId());
            } else {
                body.put("accountNumber", request.getAccountNumber());
                body.put("bankId", request.getBankId());
                body.put("country", request.getCountry() == null ? "zm" : request.getCountry());
            }
        } else {
            path = "/transfers/mobile-money";
            if (request.getTransferRecipientId() != null && !request.getTransferRecipientId().trim().isEmpty()) {
                body.put("transferRecipientId", request.getTransferRecipientId());
            } else {
                body.put("phone", request.getPhone());
                body.put("operator", request.getOperator());
                body.put("country", request.getCountry() == null ? "zm" : request.getCountry());
            }
        }

        try {
            JsonNode root = webClient.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            JsonNode data = root == null ? null : root.path("data");
            String status = data == null ? "FAILED" : data.path("status").asText("FAILED").toUpperCase();
            String message = root == null ? "Empty response from Lenco." : root.path("message").asText("");
            String lencoReference = data == null ? null : data.path("lencoReference").asText(null);

            boolean accepted = "PENDING".equals(status) || "SUCCESSFUL".equals(status);
            return new LencoSettlementResponse(accepted, status, message, lencoReference);
        } catch (WebClientResponseException e) {
            log.error("Lenco payout request failed: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 401) {
                return new LencoSettlementResponse(false, "FAILED", "Lenco authorization failed.", null);
            }
            LencoSettlementResponse status = getStatus(request.getReference());
            if ("SUCCESSFUL".equalsIgnoreCase(status.getStatus()) || "PENDING".equalsIgnoreCase(status.getStatus())) {
                return status;
            }
            return new LencoSettlementResponse(false, "FAILED", "Lenco payout request failed.", null);
        } catch (Exception e) {
            log.error("Unexpected Lenco payout error; outcome cannot be confirmed", e);
            return new LencoSettlementResponse(true, "PENDING", "Lenco payout outcome is being reconciled.", null);
        }
    }
}
