package org.membershipcommons.entity.finance;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;
import org.membershipcommons.entity.IdBasedEntity;

import javax.persistence.*;
import java.util.Objects;

@Data
@NoArgsConstructor
@Entity
@Table(name = "general_invoice_items")
public class GeneralInvoiceItem extends IdBasedEntity {

    @Column(name = "item_no", length = 245, nullable = false)
    public String itemNo;
    @ManyToOne
    @JoinColumn(name = "general_invoice_id")
    private GeneralInvoice generalInvoice;
    @ManyToOne
    @JoinColumn(name = "payment_type_id")
    private PaymentType paymentType;
    @Column(name = "description", length = 245, nullable = false)
    private String description;
    @Column(name = "amount", nullable = false)
    private float amount;

    private boolean paid;

    public GeneralInvoiceItem(Integer id, String itemNo, PaymentType type, String description, float amount, GeneralInvoice invoice) {
        super();
        this.id = id;
        this.itemNo = itemNo;
        this.paymentType = type;
        this.description = description;
        this.amount = amount;
        this.generalInvoice = invoice;
    }

    public GeneralInvoiceItem(String itemNo, PaymentType type, String description, float amount, GeneralInvoice invoice) {
        this.itemNo = itemNo;
        this.paymentType = type;
        this.description = description;
        this.amount = amount;
        this.generalInvoice = invoice;
    }


    @Override
    public String toString() {
        return itemNo + " ~ " + amount;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        GeneralInvoiceItem that = (GeneralInvoiceItem) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
