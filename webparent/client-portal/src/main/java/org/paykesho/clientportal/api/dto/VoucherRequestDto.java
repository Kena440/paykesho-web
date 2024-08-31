package org.paykesho.clientportal.api.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VoucherRequestDto {
    private BigDecimal amount;
    private String reason;
    //private String payslipFileName;
}
