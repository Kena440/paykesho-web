package org.paykesho.paymentservice.payments.dto.momo;

import lombok.Data;

@Data
public class MoMoPaymentToMerchantRequest {
    private String referenceNo;
    private String amount;
    private String receiverId;
}
