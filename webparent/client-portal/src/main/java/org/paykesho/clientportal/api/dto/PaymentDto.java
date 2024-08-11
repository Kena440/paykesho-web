package org.paykesho.clientportal.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PaymentDto {
    private String merchantCode;
    private String voucherCode;
    private Integer clientId;
    private BigDecimal transactionAmount;
}
