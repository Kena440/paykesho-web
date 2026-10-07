package org.paykesho.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LencoSettlementResponse {
    private boolean accepted;
    private String status;
    private String message;
    private String lencoReference;
}
