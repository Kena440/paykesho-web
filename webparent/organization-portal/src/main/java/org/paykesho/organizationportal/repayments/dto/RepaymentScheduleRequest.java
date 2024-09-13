package org.paykesho.organizationportal.repayments.dto;

import lombok.Data;
import org.commons.entity.client.Client;

import javax.persistence.Transient;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
public class RepaymentScheduleRequest {
    private String reference;
    private Client client;
    private BigDecimal amountDue;
    private Date dueDate;

    @Transient
    public String getDueDateOnForm() {
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        return dateFormatter.format(this.dueDate);
    }

    public void setDueDateOnForm(String dateString) {
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");

        try {
            this.dueDate = dateFormatter.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}