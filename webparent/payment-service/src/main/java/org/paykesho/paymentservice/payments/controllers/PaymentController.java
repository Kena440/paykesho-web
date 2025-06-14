package org.paykesho.paymentservice.payments.controllers;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/pay/bank-transfer")
    public ResponseEntity<BankPaymentResponse> processPayment(@RequestBody BankPaymentRequest bankPaymentRequest) {
        BankPaymentResponse bankPaymentResponse = paymentService.runWalletToBankPayment(bankPaymentRequest);
        return ResponseEntity.ok(bankPaymentResponse);
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