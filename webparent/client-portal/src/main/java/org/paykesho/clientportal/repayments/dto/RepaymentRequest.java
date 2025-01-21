package org.paykesho.clientportal.repayments.dto;

import lombok.Data;
import org.commons.entity.client.Client;

import java.math.BigDecimal;

@Data
public class RepaymentRequest {
    private Client client;
    private String reference;
    private BigDecimal amount;
}
