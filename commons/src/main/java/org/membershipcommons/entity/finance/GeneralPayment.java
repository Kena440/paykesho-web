package org.membershipcommons.entity.finance;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.membershipcommons.entity.IdBasedEntity;
import org.membershipcommons.entity.User;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "general_payments")
public class GeneralPayment extends IdBasedEntity {

    @ManyToOne
    @JoinColumn(name = "member_invoice_id")
    private GeneralInvoice generalInvoice;
    @Column(name = "serial", length = 245)
    private String serial;
    @Column(name = "amount", nullable = false)
    private float amount;
    @OneToMany(mappedBy = "generalPayment", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GeneralPaymentItem> paymentItems = new HashSet<>();
    private Date paymentDate;
    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;
    private Date createdDate;

    public void addItems(Integer id, String itemNo, String description, float amount, User user, Date date) {
        this.paymentItems.add(new GeneralPaymentItem(id, itemNo, description, amount, this, user, date));
    }

    public void addItems(String itemNo, String description, float amount, User user, Date date) {
        this.paymentItems.add(new GeneralPaymentItem(itemNo, description, amount, this, user, date));
    }

    @Override
    public String toString() {
        return serial + " ~ " + amount;
    }

}
