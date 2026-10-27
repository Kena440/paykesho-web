package org.paykesho.clientportal.settlement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor // needed to read the payment service's JSON reply
public class SettlementClientResponse {
    private boolean accepted;
    private String status;
    private String message;
    private String lencoReference;
}
