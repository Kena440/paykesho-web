package org.membershipcommons.entity.iga;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;
import org.membershipcommons.entity.IdBasedEntity;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

@Data
@NoArgsConstructor
@Entity
@Table(name = "iga_loan_items")
public class IGALoanRepaymentItem extends IdBasedEntity {

    @ManyToOne
    @JoinColumn(name = "iga_loan_id")
    private IGALoan igaLoan;
    @Column(name = "reference", length = 245)
    private String reference;
    @Column(name = "note", length = 245)
    private String note;
    private Date period;
    @Column(name = "principal_paid", nullable = false)
    private float principalPaid;
    @Column(name = "interest_paid", nullable = false)
    private float interestPaid;
    @Column(name = "principal_balance", nullable = false)
    private float principalBalance;
    @Column(name = "interest_balance", nullable = false)
    private float interestBalance;

    private boolean paid;

    public IGALoanRepaymentItem(Integer id, Date period, float principalPaid, float interestPaid, float principalBalance, float interestBalance, String reference, String note, IGALoan igaLoan) {
        super();
        this.id = id;
        this.period = period;
        this.principalPaid = principalPaid;
        this.interestPaid = interestPaid;
        this.principalBalance = principalBalance;
        this.interestBalance = interestBalance;
        this.reference = reference;
        this.note = note;
        this.igaLoan = igaLoan;
    }

    public IGALoanRepaymentItem(Date period, float principalPaid, float interestPaid, float principalBalance, float interestBalance, String reference, String note, IGALoan igaLoan) {
        this.igaLoan = igaLoan;
        this.period = period;
        this.principalPaid = principalPaid;
        this.interestPaid = interestPaid;
        this.principalBalance = principalBalance;
        this.interestBalance = interestBalance;
        this.reference = reference;
        this.note = note;
    }

    @Override
    public String toString() {
        return principalPaid + " ~ " + interestPaid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        IGALoanRepaymentItem that = (IGALoanRepaymentItem) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
