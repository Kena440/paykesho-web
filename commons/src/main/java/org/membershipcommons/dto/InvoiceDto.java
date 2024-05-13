package org.membershipcommons.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDto {
    private Integer id;
    private String serial;
    private String currency;
    private float netAmount;
    private String invoiceDate;
}
