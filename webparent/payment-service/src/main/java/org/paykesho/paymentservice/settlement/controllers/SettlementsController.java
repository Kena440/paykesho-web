package org.paykesho.paymentservice.settlement.controllers;

import org.commons.utils.PaykeshoError;
import org.paykesho.paymentservice.payments.dto.bank.BankPaymentRequest;
import org.paykesho.paymentservice.payments.dto.bank.BankPaymentResponse;
import org.paykesho.paymentservice.payments.services.PaymentService;
import org.paykesho.paymentservice.settlement.scheduler.SettlementScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/api/settlements")
public class SettlementsController {
    private final SettlementScheduler settlementScheduler;

    public SettlementsController(SettlementScheduler settlementScheduler) {
        this.settlementScheduler = settlementScheduler;
    }

    @PostMapping("/process-settlements")
    public ResponseEntity<String> processSettlements() throws PaykeshoError {
        settlementScheduler.settleAllMerchants();
        return ResponseEntity.ok("done");
    }

}