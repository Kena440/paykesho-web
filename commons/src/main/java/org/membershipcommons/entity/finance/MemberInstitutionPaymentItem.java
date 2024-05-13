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
@Table(name = "member_payment_items")
public class MemberInstitutionPaymentItem extends IdBasedEntity {

    @ManyToOne
    @JoinColumn(name = "member_institution_payment_id")
    private MemberInstitutionPayment memberInstitutionPayment;
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

    public MemberInstitutionPaymentItem(Integer id, String itemNo, String description, float amount, MemberInstitutionPayment memberInstitutionPayment, User updator, Date updatedDate) {
        super();
        this.id = id;
        this.itemNo = itemNo;
        this.description = description;
        this.amount = amount;
        this.memberInstitutionPayment = memberInstitutionPayment;
        this.updator = updator;
        this.updatedDate = updatedDate;
    }

    public MemberInstitutionPaymentItem(String itemNo, String description, float amount, MemberInstitutionPayment memberInstitutionPayment, User updator, Date updatedDate) {
        this.itemNo = itemNo;
        this.description = description;
        this.amount = amount;
        this.memberInstitutionPayment = memberInstitutionPayment;
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
        MemberInstitutionPaymentItem that = (MemberInstitutionPaymentItem) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
