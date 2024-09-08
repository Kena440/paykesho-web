package org.paykesho.backoffice.repayments.dto;

import lombok.Data;
import org.commons.entity.client.Client;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class RepaymentScheduleRequest {
    private String reference;
    private Client client;
    private BigDecimal amountDue;
    private Date dueDate;
}