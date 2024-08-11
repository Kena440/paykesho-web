package org.paykesho.clientportal.api.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VoucherRequestDto {
    private String merchant;
    private BigDecimal voucherAmount;
    //private String payslipFileName;
    private String clientPaykeshoID;


}
