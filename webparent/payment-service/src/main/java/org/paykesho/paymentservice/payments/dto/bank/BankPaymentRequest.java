package org.paykesho.paymentservice.payments.dto.bank;

import lombok.Data;

@Data
public class BankPaymentRequest {
    private String referenceNo;
    private String amount;
    private String description;
    private String bankName;
    private String receiverId;
}
