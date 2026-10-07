package org.paykesho.clientportal.settlement;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SettlementClientResponse {
    private boolean accepted;
    private String status;
    private String message;
    private String lencoReference;
}
