package org.paykesho.paymentservice.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LencoSettlementRequest {
    private BigDecimal amount;
    private String reference;
    private String narration;
    private String payoutMethod;
    private String transferRecipientId;
    private String phone;
    private String operator;
    private String country;
    private String bankId;
    private String accountNumber;
}
