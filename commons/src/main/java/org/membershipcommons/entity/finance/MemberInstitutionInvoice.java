package org.membershipcommons.entity.finance;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.membershipcommons.entity.IdBasedEntity;
import org.membershipcommons.entity.User;
import org.membershipcommons.entity.members.MemberInstitution;
import org.membershipcommons.entity.settings.Currency;
import org.membershipcommons.enums.InvoiceType;

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
@Table(name = "member_invoice")
public class MemberInstitutionInvoice extends IdBasedEntity {

    @ManyToOne
    @JoinColumn(name = "member_institution_id")
    private MemberInstitution memberInstitution;
    @ManyToOne
    @JoinColumn(name = "currency_id")
    private Currency currency;
    @ManyToOne
    @JoinColumn(name = "payment_account_id")
    private PaymentAccount paymentAccount;
    @Column(name = "serial", length = 245)
    private String serial;
    @Column(name = "net_amount", nullable = false)
    private float netAmount;
    @Column(name = "amount_without_bff", nullable = false)
    private float amountWithoutBFF;
    private Date invoiceDate;
    private Date invoiceDueDate;
    @OneToMany(mappedBy = "memberInstitutionInvoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MemberInstitutionInvoiceItem> invoiceItems = new HashSet<>();

    private boolean paid;
    private boolean received;
    private boolean approved;
    private boolean archived;
    private Date paidDate;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;
    private Date createdDate;
    @ManyToOne
    @JoinColumn(name = "updator_id")
    private User updator;
    private Date updatedDate;
    @Enumerated(EnumType.STRING)
    private InvoiceType invoiceType;

    public void addItems(Integer id, String itemNo, PaymentType type, String description, float amount) {
        this.invoiceItems.add(new MemberInstitutionInvoiceItem(id, itemNo, type, description, amount, this));
    }

    public void addItems(String itemNo, PaymentType type, String description, float amount) {
        this.invoiceItems.add(new MemberInstitutionInvoiceItem(itemNo, type, description, amount, this));
    }

    @Override
    public String toString() {
        return serial + " ~ " + netAmount;
    }

    @Transient
    public String getIdOnForm() {
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormatter.format(this.invoiceDate);
    }

    public void setIdOnForm(String dateString) {
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

        try {
            this.invoiceDate = dateFormatter.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Transient
    public String getIddOnForm() {
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormatter.format(this.invoiceDueDate);
    }

    public void setIddOnForm(String dateString) {
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

        try {
            this.invoiceDueDate = dateFormatter.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
