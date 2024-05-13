package org.membershipcommons.entity.finance;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.membershipcommons.entity.IdBasedEntity;
import org.membershipcommons.enums.PaymentClass;

import javax.persistence.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "payment_types")
public class PaymentType extends IdBasedEntity {
    @Column(length = 64, nullable = false)
    private String name;
    @Column(length = 64, nullable = false, unique = true)
    private String code;
    @Enumerated(EnumType.STRING)
    private PaymentClass paymentClass;

    public PaymentType(String name, String code, PaymentClass paymentClass) {
        this.name = name;
        this.code = code;
        this.paymentClass = paymentClass;
    }

    @Override
    public String toString() {
        return code + " " + name;
    }
}
