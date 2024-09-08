package org.paykesho.organizationportal.repayments.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RepaymentRequest {
    private String reference;
    private BigDecimal amount;
}
