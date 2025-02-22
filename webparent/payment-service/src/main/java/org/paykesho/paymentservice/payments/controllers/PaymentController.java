package org.paykesho.paymentservice.payments.controllers;

import org.paykesho.paymentservice.payments.dto.bank.BankPaymentRequest;
import org.paykesho.paymentservice.payments.dto.bank.BankPaymentResponse;
import org.paykesho.paymentservice.payments.dto.momo.MoMoPaymentRequest;
import org.paykesho.paymentservice.payments.dto.momo.MoMoPaymentResponse;
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

    @PostMapping("/pay/momo")
    public ResponseEntity<MoMoPaymentResponse> processPayment(@RequestBody MoMoPaymentRequest moMoPaymentRequest) {
        MoMoPaymentResponse moMoPaymentResponse = paymentService.runMoMoPayment(moMoPaymentRequest);
        return ResponseEntity.ok(moMoPaymentResponse);
    }
}