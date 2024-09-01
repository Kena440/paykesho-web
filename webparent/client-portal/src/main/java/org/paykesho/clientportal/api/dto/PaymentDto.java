package org.paykesho.clientportal.api.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentDto {
    private String merchantCode;
    private Integer clientId;
    private BigDecimal amount;
}
