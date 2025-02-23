package org.commons.dto;

import lombok.Data;

@Data
public class MoMoPaymentToPaykeshoRequest {
    private String referenceNo;
    private String amount;
    private String senderId;
}
