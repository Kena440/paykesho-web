package org.paykesho.clientportal.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
public class TransactionDto {
    private String paykeshoReference;
    private String merchant;
    private BigDecimal transactionAmount;
    private Date transactionDate;
    private String voucher;
    private String transactionNaration;
}
