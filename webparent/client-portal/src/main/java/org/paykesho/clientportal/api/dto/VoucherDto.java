package org.paykesho.clientportal.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
public class VoucherDto {
    private String reference;
    private BigDecimal voucherAmount;
    private BigDecimal voucherAvailableAmount;
    private BigDecimal voucherRepaymentAmount;
    private BigDecimal voucherInterestPercent;
    private String voucherRepaymentCycle;
    private Date voucherRepaymentDate;
    private Date voucherRequestDate;
    private boolean approved;
}
