package org.membershipcommons.entity.finance;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;
import org.membershipcommons.entity.IdBasedEntity;
import org.membershipcommons.entity.User;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

@Data
@NoArgsConstructor
@Entity
@Table(name = "general_payment_items")
public class GeneralPaymentItem extends IdBasedEntity {

    @ManyToOne
    @JoinColumn(name = "general_payment_id")
    private GeneralPayment generalPayment;
    @Column(name = "item_no", length = 245, nullable = false)
    private String itemNo;
    @Column(name = "description", length = 245, nullable = false)
    private String description;
    @Column(name = "amount", nullable = false)
    private float amount;

    @ManyToOne
    @JoinColumn(name = "updator_id")
    private User updator;
    private Date updatedDate;

    public GeneralPaymentItem(Integer id, String itemNo, String description, float amount, GeneralPayment generalPayment, User updator, Date updatedDate) {
        super();
        this.id = id;
        this.itemNo = itemNo;
        this.description = description;
        this.amount = amount;
        this.generalPayment = generalPayment;
        this.updator = updator;
        this.updatedDate = updatedDate;
    }

    public GeneralPaymentItem(String itemNo, String description, float amount, GeneralPayment generalPayment, User updator, Date updatedDate) {
        this.itemNo = itemNo;
        this.description = description;
        this.amount = amount;
        this.generalPayment = generalPayment;
        this.updator = updator;
        this.updatedDate = updatedDate;
    }


    @Override
    public String toString() {
        return itemNo + " ~ " + amount;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        GeneralPaymentItem that = (GeneralPaymentItem) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
