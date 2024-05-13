package org.membershipcommons.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.membershipcommons.entity.IdBasedEntity;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentItemDto extends IdBasedEntity {
    private Integer paymentId;
    private Integer invoiceId;
    private String description;
    private float amount;
}
