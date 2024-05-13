package org.membershipcommons.entity.initiative;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;
import org.membershipcommons.entity.IdBasedEntity;
import org.membershipcommons.entity.members.MemberInstitution;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

@Data
@NoArgsConstructor
@Entity
@Table(name = "initiative_payment_items")
public class InitiativePaymentItem extends IdBasedEntity {

    @ManyToOne
    @JoinColumn(name = "initiative_id")
    private Initiative initiative;
    @ManyToOne
    @JoinColumn(name = "member_institution_id")
    private MemberInstitution memberInstitution;
    @Column(name = "description", length = 245, nullable = false)
    private String description;
    private Date paymentDate;
    @Column(name = "amount", nullable = false)
    private float amount;

    private boolean paid;

    public InitiativePaymentItem(Integer id, Date paymentDate, String description, float amount, MemberInstitution institution, Initiative initiative) {
        super();
        this.id = id;
        this.paymentDate = paymentDate;
        this.description = description;
        this.amount = amount;
        this.initiative = initiative;
        this.memberInstitution = institution;
    }

    public InitiativePaymentItem(Date paymentDate, String description, float amount, MemberInstitution institution, Initiative initiative) {
        this.paymentDate = paymentDate;
        this.description = description;
        this.amount = amount;
        this.initiative = initiative;
        this.memberInstitution = institution;

    }

    @Override
    public String toString() {
        return initiative + " ~ " + amount + " ~ " + paymentDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        InitiativePaymentItem that = (InitiativePaymentItem) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
