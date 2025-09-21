package org.paykesho.paymentservice.payments.controllers;

import lombok.extern.slf4j.Slf4j;
import org.commons.dto.CardPaymentInitialResponse;
import org.commons.dto.CardPaymentRequest;
import org.commons.dto.MoMoPaymentToPaykeshoRequest;
import org.commons.dto.MoMoPaymentToPaykeshoResponse;
import org.paykesho.paymentservice.payments.dto.bank.BankPaymentRequest;
import org.paykesho.paymentservice.payments.dto.bank.BankPaymentResponse;
import org.paykesho.paymentservice.payments.dto.card.CardPaymentCallBackResponse;
import org.paykesho.paymentservice.payments.dto.momo.MoMoPaymentToMerchantRequest;
import org.paykesho.paymentservice.payments.dto.momo.MoMoPaymentToMerchantResponse;
import org.paykesho.paymentservice.payments.services.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/rest/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/pay/bank-transfer")
    public ResponseEntity<BankPaymentResponse> processPayment(
            @RequestBody BankPaymentRequest bankPaymentRequest,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        try {
            // SECURITY CHECK #1: Validate idempotency key for duplicate prevention
            if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorBankResponse("Idempotency-Key header is required"));
            }

            // Check if this payment was already processed
            if (paymentService.isPaymentAlreadyProcessed(idempotencyKey)) {
                log.warn("Duplicate payment attempt detected with idempotency key: {}", idempotencyKey);
                return ResponseEntity.badRequest().body(createErrorBankResponse("Payment already processed"));
            }

            // Input validation
            if (bankPaymentRequest == null) {
                return ResponseEntity.badRequest().body(createErrorBankResponse("Request body cannot be null"));
            }

            if (bankPaymentRequest.getAmount() == null || bankPaymentRequest.getAmount().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorBankResponse("Amount is required"));
            }

            if (bankPaymentRequest.getReceiverId() == null || bankPaymentRequest.getReceiverId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorBankResponse("Receiver ID is required"));
            }

            // Validate amount format
            try {
                double amount = Double.parseDouble(bankPaymentRequest.getAmount());
                if (amount <= 0) {
                    return ResponseEntity.badRequest().body(createErrorBankResponse("Amount must be greater than 0"));
                }
                if (amount > 1000000) { // Max transaction limit
                    return ResponseEntity.badRequest().body(createErrorBankResponse("Amount exceeds maximum transaction limit"));
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(createErrorBankResponse("Invalid amount format"));
            }

            BankPaymentResponse bankPaymentResponse = paymentService.runWalletToBankPayment(bankPaymentRequest);
            return ResponseEntity.ok(bankPaymentResponse);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(createErrorBankResponse("Payment processing failed: " + e.getMessage()));
        }
    }

    private BankPaymentResponse createErrorBankResponse(String errorMessage) {
        BankPaymentResponse errorResponse = new BankPaymentResponse();
        errorResponse.setResponse_code("999");
        errorResponse.setResponse_description(errorMessage);
        return errorResponse;
    }

    @PostMapping("/pay/momo/to-merchant")
    public ResponseEntity<MoMoPaymentToMerchantResponse> processPayment(@RequestBody MoMoPaymentToMerchantRequest moMoPaymentToMerchantRequest) {
        MoMoPaymentToMerchantResponse moMoPaymentToMerchantResponse = paymentService.runMoMoPaymentToMerchant(moMoPaymentToMerchantRequest);
        return ResponseEntity.ok(moMoPaymentToMerchantResponse);
    }

    @PostMapping("/pay/momo/to-paykesho")
    public ResponseEntity<MoMoPaymentToPaykeshoResponse> processPayment(@RequestBody MoMoPaymentToPaykeshoRequest moMoPaymentToPaykeshoRequest) {
        MoMoPaymentToPaykeshoResponse moMoPaymentToPaykeshoResponse = paymentService.runMoMoPaymentToPaykesho(moMoPaymentToPaykeshoRequest);
        return ResponseEntity.ok(moMoPaymentToPaykeshoResponse);
    }

    @PostMapping("/pay/card")
    public ResponseEntity<CardPaymentInitialResponse> processPayment(@RequestBody CardPaymentRequest moMoPaymentRequest) {
        CardPaymentInitialResponse cardPaymentInitialResponse = paymentService.runCardPayment(moMoPaymentRequest);
        return ResponseEntity.ok(cardPaymentInitialResponse);
    }

    @PostMapping("/callback/zynlepay")
    public ResponseEntity<String> handleZynlePayCallback(@RequestBody CardPaymentCallBackResponse callbackResponse) {
        try {
            paymentService.handleZynlePayCallback(callbackResponse);
            return ResponseEntity.ok("Callback processed successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error processing callback: " + e.getMessage());
        }
    }
}