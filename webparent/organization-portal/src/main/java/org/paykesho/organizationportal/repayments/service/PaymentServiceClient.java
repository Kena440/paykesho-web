package org.paykesho.organizationportal.repayments.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.dto.CardPaymentInitialResponse;
import org.commons.dto.CardPaymentRequest;
import org.commons.dto.MoMoPaymentToPaykeshoRequest;
import org.commons.dto.MoMoPaymentToPaykeshoResponse;
import org.commons.utils.PaykeshoError;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentServiceClient {

    private final RestTemplate restTemplate;

    public CardPaymentInitialResponse initializeCardPayment(CardPaymentRequest cardPaymentRequest) throws PaykeshoError {
        try {
            ResponseEntity<CardPaymentInitialResponse> response = restTemplate.postForEntity("http://localhost:8084/rest/api/payments/pay/card", cardPaymentRequest, CardPaymentInitialResponse.class);

            return response.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("Payment API call failed: Status Code {}, Response {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaykeshoError("Payment initialization failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during payment initialization", e);
            throw new PaykeshoError("Unexpected error during payment initialization.");
        }
    }

    public MoMoPaymentToPaykeshoResponse initializeMomoPayment(MoMoPaymentToPaykeshoRequest moMoPaymentToPaykeshoRequest) throws PaykeshoError {
        try {
            ResponseEntity<MoMoPaymentToPaykeshoResponse> response = restTemplate.postForEntity("http://localhost:8084/rest/api/payments/pay/momo/to-paykesho", moMoPaymentToPaykeshoRequest, MoMoPaymentToPaykeshoResponse.class);

            return response.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("Payment API call failed: Status Code {}, Response {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaykeshoError("Payment initialization failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during payment initialization", e);
            throw new PaykeshoError("Unexpected error during payment initialization.");
        }
    }
}
