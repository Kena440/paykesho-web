package org.membershipcommons.entity.iga;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.membershipcommons.entity.IdBasedEntity;
import org.membershipcommons.entity.User;
import org.membershipcommons.entity.members.MemberInstitution;
import org.membershipcommons.entity.settings.Currency;

import javax.persistence.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "iga_loan")
public class IGALoan extends IdBasedEntity {

    @ManyToOne
    @JoinColumn(name = "member_institution_id")
    private MemberInstitution memberInstitution;
    @ManyToOne
    @JoinColumn(name = "currency_id")
    private Currency currency;
    @Column(name = "loan_purpose", length = 245, nullable = false)
    private String loanPurpose;
    @Column(name = "serial", length = 245)
    private String serial;
    @Column(name = "principal_amount", nullable = false)
    private float principalAmount;
    @Column(name = "period_months", nullable = false)
    private float period;
    @Column(name = "interest_percent", nullable = false)
    private float interestPercent;
    @Column(name = "interest_amount", nullable = false)
    private float interestAmount;
    @Column(name = "total_repayment", nullable = false)
    private float totalRepayment;
    @Column(name = "total_owing", nullable = false)
    private float totalOwing;
    @Column(name = "total_loan_amount", nullable = false)
    private float totalLoanAmount;

    private Date loanDate;
    @OneToMany(mappedBy = "igaLoan", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<IGALoanRepaymentItem> igaLoanRepaymentItems = new HashSet<>();

    private boolean paid;
    private Date paidDate;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;
    private Date createdDate;

    public void addItems(Integer id, Date period, float principalPaid, float interestPaid, float principalBalance, float interestBalance, String reference, String note) {
        this.igaLoanRepaymentItems.add(new IGALoanRepaymentItem(id, period, principalPaid, interestPaid, principalBalance, interestBalance, reference, note, this));
    }

    public void addItems(Date period, float principalPaid, float interestPaid, float principalBalance, float interestBalance, String reference, String note) {
        this.igaLoanRepaymentItems.add(new IGALoanRepaymentItem(period, principalPaid, interestPaid, principalBalance, interestBalance, reference, note, this));
    }

    @Override
    public String toString() {
        return serial + " ~ " + principalAmount;
    }

    @Transient
    public String getLdOnForm() {
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormatter.format(this.loanDate);
    }

    public void setLdOnForm(String dateString) {
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

        try {
            this.loanDate = dateFormatter.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

}
